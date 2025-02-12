/*
 *  Copyright (C) <2022> <XiaoMoMi>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.customfishing.fishing.totem;

import net.momirealms.customfishing.CustomFishing;
import net.momirealms.customfishing.manager.FishingManager;
import net.momirealms.customfishing.util.ArmorStandUtils;
import net.momirealms.customfishing.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ActivatedTotem implements Runnable {

    public static int id = 121616121;
    private int timer;
    private final TotemConfig totem;
    private final Location bottomLoc;
    private final Location coreLoc;
    private final Set<Player> nearbyPlayerSet;
    private final int[] entityID;
    private final boolean hasHolo;
    private final TotemParticle particleTimerTask;
    private final FishingManager fishingManager;
    private final int direction;
    private final String activator;
    private final ScheduledFuture<?> totemTask;

    public ActivatedTotem(Location coreLoc, TotemConfig totem, FishingManager fishingManager, int direction, String activator) {
        this.fishingManager = fishingManager;
        this.totem = totem;
        this.coreLoc = coreLoc;
        this.bottomLoc = coreLoc.clone().subtract(0, totem.getOriginalModel().getCorePos().getY(), 0);
        this.entityID = new int[totem.getHoloText().length];
        for (int i = 0; i < totem.getHoloText().length; i++) {
            this.entityID[i] = id++;
        }
        this.hasHolo = totem.getHoloText() != null;
        this.nearbyPlayerSet = Collections.synchronizedSet(new HashSet<>());
        this.particleTimerTask = new TotemParticle(bottomLoc, totem.getRadius(), totem.getParticle());
        this.direction = direction;
        this.activator = activator;
        this.totemTask = CustomFishing.getInstance().getScheduler().runTaskTimer(this, 1, 1, TimeUnit.SECONDS, coreLoc);
    }

    @Override
    public void run() {

        timer++;
        if (timer > totem.getDuration()) {
            stop();
            return;
        }

        HashSet<Player> temp = new HashSet<>(nearbyPlayerSet);
        Collection<Player> nearbyPlayers = LocationUtils.getNearbyPlayers(bottomLoc, totem.getRadius());

        for (Player player : temp) {
            if (nearbyPlayers.remove(player)) {
                if (hasHolo) {
                    for (int i = 0; i < entityID.length; i++) {
                        CustomFishing.getProtocolManager().sendServerPacket(player, ArmorStandUtils.getMetaPacket(entityID[i],
                                totem.getHoloText()[entityID.length - 1 - i]
                                        .replace("{time}", String.valueOf(totem.getDuration() - timer))
                                        .replace("{max_time}", String.valueOf(totem.getDuration()))
                                        .replace("{player}", activator)
                        ));
                    }
                    addPotionEffect(player);
                }
            }
            else {
                if (hasHolo) {
                    for (int j : entityID) {
                        CustomFishing.getProtocolManager().sendServerPacket(player, ArmorStandUtils.getDestroyPacket(j));
                    }
                }
                nearbyPlayerSet.remove(player);
            }
        }

        for (Player newComer : nearbyPlayers) {
            if (hasHolo) {
                for (int i = 0; i < entityID.length; i++) {
                    CustomFishing.getProtocolManager().sendServerPacket(newComer, ArmorStandUtils.getSpawnPacket(entityID[i], bottomLoc.clone().add(0.5, totem.getHoloOffset() + i * 0.4, 0.5)));
                    CustomFishing.getProtocolManager().sendServerPacket(newComer, ArmorStandUtils.getMetaPacket(entityID[i],
                            totem.getHoloText()[entityID.length - 1 - i]
                                    .replace("{time}", String.valueOf(totem.getDuration() - timer))
                                    .replace("{max_time}", String.valueOf(totem.getDuration()))
                                    .replace("{player}", activator)
                    ));
                }
                addPotionEffect(newComer);
            }
            nearbyPlayerSet.add(newComer);
        }
    }

    public Set<Player> getNearbyPlayerSet() {
        return nearbyPlayerSet;
    }

    public TotemConfig getTotem() {
        return totem;
    }

    public void stop() {
        this.particleTimerTask.cancel();
        cancel();
        fishingManager.removeTotem(coreLoc);
        CustomFishing.getInstance().getTotemManager().clearBreakDetectCache(totem.getFinalModel(), bottomLoc, direction);
        if (hasHolo) {
            for (Player player : nearbyPlayerSet) {
                for (int j : entityID) {
                    CustomFishing.getProtocolManager().sendServerPacket(player, ArmorStandUtils.getDestroyPacket(j));
                }
            }
        }
        nearbyPlayerSet.clear();
    }

    private void addPotionEffect(Player player) {
        if (totem.getPotionEffects() != null) {
            for (PotionEffect potionEffect : totem.getPotionEffects()) {
                player.addPotionEffect(potionEffect);
            }
        }
    }

    public void cancel() {
        this.totemTask.cancel(false);
    }
}
