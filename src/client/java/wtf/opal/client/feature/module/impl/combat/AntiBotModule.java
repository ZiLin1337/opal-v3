package wtf.opal.client.feature.module.impl.combat;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.world.GameMode;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.module.Module;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.property.impl.number.NumberProperty;
import wtf.opal.event.impl.game.PreGameTickEvent;
import wtf.opal.event.impl.game.packet.ReceivePacketEvent;
import wtf.opal.event.impl.game.JoinWorldEvent;
import wtf.opal.event.subscriber.Subscribe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static wtf.opal.client.Constants.mc;

public final class AntiBotModule extends Module {

    private final NumberProperty respawnTimeValue = new NumberProperty("Respawn Time", 2500.0, 0.0, 10000.0, 100.0);

    private final Map<UUID, String> uuidDisplayNames = new ConcurrentHashMap<>();
    private final Map<Integer, String> entityIdDisplayNames = new ConcurrentHashMap<>();
    private final Map<UUID, Long> uuids = new ConcurrentHashMap<>();
    private final Set<Integer> ids = new HashSet<>();
    private final Map<UUID, Long> respawnTime = new ConcurrentHashMap<>();

    public AntiBotModule() {
        super("AntiBot", "Prevents attacking bots and watcher entities.", ModuleCategory.COMBAT);
        addProperties(respawnTimeValue);
    }

    public static boolean isBot(Entity entity) {
        if (mc.player == null || mc.world == null) return false;

        AntiBotModule module = OpalClient.getInstance().getModuleRepository().getModule(AntiBotModule.class);
        if (module == null) {
            return false;
        }
        if (!module.isEnabled()) {
            return false;
        }

        if (module.ids.contains(entity.getId())) {
            return true;
        }

        if (mc.getNetworkHandler() != null) {
            if (mc.getNetworkHandler().getPlayerListEntry(entity.getUuid()) == null) {
                return true;
            }
        }

        if (module.respawnTimeValue.getValue() >= 1.0) {
            if (module.respawnTime.containsKey(entity.getUuid())) {
                long timeDiff = System.currentTimeMillis() - module.respawnTime.get(entity.getUuid());
                return timeDiff < module.respawnTimeValue.getValue();
            }
        }

        return false;
    }

    @Subscribe
    public void onJoinWorld(JoinWorldEvent event) {
        clearData();
    }

    @Subscribe
    public void onPreGameTick(PreGameTickEvent event) {
        long now = System.currentTimeMillis();
        uuids.entrySet().removeIf(entry -> now - entry.getValue() > 500L);
    }

    @Subscribe
    public void onPacketReceive(ReceivePacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                UUID id = entry.profileId();

                respawnTime.put(id, System.currentTimeMillis());

                var displayName = entry.displayName();
                var gameMode = entry.gameMode();

                if (displayName != null && displayName.getSiblings().isEmpty() && gameMode == GameMode.SURVIVAL) {
                    uuids.put(id, System.currentTimeMillis());
                    uuidDisplayNames.put(id, displayName.getString());
                }
            }
        } else if (event.getPacket() instanceof EntityAnimationS2CPacket packet) {
            if (packet.getAnimationId() == 0) {
                Entity entity = mc.world.getEntityById(packet.getEntityId());
                if (entity != null) {
                    respawnTime.remove(entity.getUuid());
                }
            }
        } else if (event.getPacket() instanceof EntitySpawnS2CPacket packet) {
            UUID uuid = packet.getUuid();
            int entityId = packet.getEntityId();

            if (uuids.containsKey(uuid)) {
                String displayName = uuidDisplayNames.get(uuid);
                entityIdDisplayNames.put(entityId, displayName);
                uuids.remove(uuid);
                ids.add(entityId);
            }
        } else if (event.getPacket() instanceof EntitiesDestroyS2CPacket packet) {
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