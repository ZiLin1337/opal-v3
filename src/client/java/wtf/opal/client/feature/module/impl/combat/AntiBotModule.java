package wtf.opal.client.feature.module.impl.combat;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.world.GameMode;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.impl.game.world.WorldLoadEvent;
import wtf.opal.event.subscriber.Subscribe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static wtf.opal.client.Constants.mc;

public final class AntiBotModule extends Module {

    private final NumberProperty respawnTimeValue = new NumberProperty("Respawn Time", 2500.0, 0.0, 10000.0, 100.0);

    // 缓存数据
    private final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
    private final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
    private final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    private final Set<Integer> ids = new HashSet<>();
    private final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();

    public AntiBotModule() {
        super("AntiBot", "Prevents attacking bots and watcher entities.", ModuleCategory.COMBAT);
        addProperties(respawnTimeValue);
    }

    /**
     * 判断实体是否为机器人 (供 TargetList 调用)
     */
    public static boolean isBot(Entity entity) {
        if (mc.player == null || mc.world == null) return false;

        AntiBotModule module = OpalClient.getInstance().getModuleRepository().getModule(AntiBotModule.class);
        if (!module.isEnabled()) {
            return false;
        }

        // 1. 检查是否在 IDs 列表中 (Advanced Bot 检测)
        if (module.ids.contains(entity.getId())) {
            return true;
        }

        // 2. 检查 Tab 列表是否存在 (基础反人机)
        if (mc.getNetworkHandler() != null && !mc.getNetworkHandler().getPlayerList().stream()
                .anyMatch(entry -> entry.getProfile().getId().equals(entity.getUuid()))) {
            return true;
        }

        // 3. BedWars Bot 检测 (重生时间检查)
        if (module.respawnTimeValue.getValue() >= 1.0) {
            if (module.respawnTime.containsKey(entity.getUuid())) {
                long timeDiff = System.currentTimeMillis() - module.respawnTime.get(entity.getUuid());
                return timeDiff < module.respawnTimeValue.getValue();
            }
        }

        return false;
    }

    @Subscribe
    public void onWorldLoad(WorldLoadEvent event) {
        clearData();
    }

    @Subscribe
    public void onPreGameTick(PreGameTickEvent event) {
        // 清理超时的临时 UUID 记录 (对应 Heypixel 的 onMotion)
        long now = System.currentTimeMillis();
        uuids.entrySet().removeIf(entry -> now - entry.getValue() > 500L);
    }

    @Subscribe
    public void onPacketReceive(ReceivePacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        // 对应 ClientboundPlayerInfoUpdatePacket
        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            if (packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) {
                for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                    GameProfile profile = entry.getProfile();
                    UUID id = profile.getId();

                    // 记录添加时间用于 BedWars Bot 检测
                    respawnTime.put(id, System.currentTimeMillis());

                    // Advanced Bot 逻辑: 检查显示名称和游戏模式
                    // Heypixel 源码: displayName().getSiblings().isEmpty() && gameMode == SURVIVAL
                    // 注意: Fabric mapping 可能略有不同，这里检查是否为纯文本名
                    if (entry.getDisplayName() != null && entry.getDisplayName().getSiblings().isEmpty() && entry.getGameMode() == GameMode.SURVIVAL) {
                        uuids.put(id, System.currentTimeMillis());
                        uuidDisplayNames.put(id, entry.getDisplayName().getString());
                    }
                }
            }
        }
        // 对应 ClientboundAnimatePacket (实体挥手/动作)
        else if (event.getPacket() instanceof EntityAnimationS2CPacket packet) {
            // 如果实体挥手了 (Action ID 0 = SWING_MAIN_HAND)，说明可能是真人，移除 BedWars Bot 标记
            if (packet.getAnimationId() == 0) {
                Entity entity = mc.world.getEntityById(packet.getEntityId());
                if (entity != null) {
                    respawnTime.remove(entity.getUuid());
                }
            }
        }
        // 对应 ClientboundAddPlayerPacket (实体生成)
        else if (event.getPacket() instanceof PlayerSpawnS2CPacket packet) {
            UUID uuid = packet.getPlayerUuid();
            if (uuids.containsKey(uuid)) {
                String displayName = uuidDisplayNames.get(uuid);
                entityIdDisplayNames.put(packet.getId(), displayName);
                uuids.remove(uuid);
                ids.add(packet.getId()); // 标记为机器人
            }
        }
        // 对应 ClientboundRemoveEntitiesPacket
        else if (event.getPacket() instanceof EntitiesDestroyS2CPacket packet) {
            for (int entityId : packet.getEntityIds()) {
                if (ids.contains(entityId)) {
                    entityIdDisplayNames.remove(entityId);
                    ids.remove(entityId);
                }
            }
        }
    }

    @Override
    public void onEnable() {
        clearData();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        clearData();
        super.onDisable();
    }

    private void clearData() {
        uuidDisplayNames.clear();
        entityIdDisplayNames.clear();
        ids.clear();
        uuids.clear();
        respawnTime.clear();
    }
}