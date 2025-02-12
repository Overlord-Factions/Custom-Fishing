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

package net.momirealms.customfishing.fishing.mode;

import net.momirealms.customfishing.CustomFishing;
import net.momirealms.customfishing.fishing.bar.ModeTwoBar;
import net.momirealms.customfishing.manager.FishingManager;
import net.momirealms.customfishing.util.AdventureUtils;
import net.momirealms.customfishing.util.LocationUtils;
import org.bukkit.Location;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class ModeTwoGame extends FishingGame {

    private boolean success;
    private double hold_time;
    private final ModeTwoBar modeTwoBar;
    private double judgement_position;
    private double fish_position;
    private double judgement_velocity;
    private double fish_velocity;
    private int timer;
    private final int time_requirement;
    private final Location hookLoc;
    private double distance;

    public ModeTwoGame(CustomFishing plugin, FishingManager fishingManager, long deadline, Player player, int difficulty, ModeTwoBar modeTwoBar, Location hookLoc) {
        super(plugin, fishingManager, deadline, player, difficulty, modeTwoBar);
        this.success = false;
        this.judgement_position = (double) (modeTwoBar.getBar_effective_width() - modeTwoBar.getJudgement_area_width()) / 2;
        this.fish_position = 0;
        this.timer = 0;
        this.modeTwoBar = modeTwoBar;
        this.time_requirement = modeTwoBar.getRandomTimeRequirement();
        this.hookLoc = hookLoc;
        this.distance = LocationUtils.getDistance(player.getLocation(), hookLoc);
        this.gameTask = plugin.getScheduler().runTaskTimer(this, 50, 33, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        super.run();
        if (modeTwoBar.isSneakMode()) {
            if (player.isSneaking()) addV(true);
            else reduceV();
        } else {
            double newDistance = LocationUtils.getDistance(player.getLocation(), hookLoc);
            if (distance < newDistance) addV(true);
            else if (distance > newDistance) addV(false);
            distance = newDistance;
        }
        if (timer < 30) {
            timer++;
        } else {
            timer = 0;
            if (Math.random() > ((double) 1 / (difficulty + 1))) {
                burst();
            }
        }
        judgement_position += judgement_velocity;
        fish_position += fish_velocity;

        fraction();
        calibrate();

        if (fish_position >= judgement_position - 2 && fish_position + modeTwoBar.getFish_icon_width() <= judgement_position + modeTwoBar.getJudgement_area_width() + 2) {
            hold_time += 0.66;
        } else {
            hold_time -= modeTwoBar.getPunishment() * 0.66;
        }
        if (hold_time >= time_requirement) {
            success = true;
            FishHook fishHook = fishingManager.getHook(player.getUniqueId());
            if (fishHook != null) {
                fishingManager.proceedReelIn(fishHook.getLocation(), player, this);
                fishingManager.removeHook(player.getUniqueId());
            }
            fishingManager.removeFishingPlayer(player);
            cancel();
            return;
        }
        showBar();
    }

    @Override
    public void showBar() {
        String bar = "<font:" + modeTwoBar.getFont() + ">" + modeTwoBar.getBarImage()
                + "<font:" + offsetManager.getFont() + ">" + offsetManager.getOffsetChars((int) (modeTwoBar.getJudgement_area_offset() + judgement_position)) + "</font>"
                + modeTwoBar.getJudgement_area_image()
                + "<font:" + offsetManager.getFont() + ">" + offsetManager.getOffsetChars((int) (modeTwoBar.getBar_effective_width() - judgement_position - modeTwoBar.getJudgement_area_width())) + "</font>"
                + "<font:" + offsetManager.getFont() + ">" + offsetManager.getOffsetChars((int) (-modeTwoBar.getBar_effective_width() - 1 + fish_position)) + "</font>"
                + modeTwoBar.getFish_image()
                + "<font:" + offsetManager.getFont() + ">" + offsetManager.getOffsetChars((int) (modeTwoBar.getBar_effective_width() - fish_position - modeTwoBar.getFish_icon_width() + 1)) + "</font>"
                + "</font>";
        hold_time = Math.max(0, Math.min(hold_time, time_requirement));
        AdventureUtils.playerTitle(player,
                title.replace("{progress}", modeTwoBar.getProgress()[(int) ((hold_time / time_requirement) * modeTwoBar.getProgress().length)])
                , bar,0,500,0
        );
    }

    private void burst() {
        if (Math.random() < (judgement_position / modeTwoBar.getBar_effective_width())) {
            judgement_velocity = -1 - 0.8 * Math.random() * difficulty;
        } else {
            judgement_velocity = 1 + 0.8 * Math.random() * difficulty;
        }
    }

    private void fraction() {
        if (judgement_velocity > 0) {
            judgement_velocity -= modeTwoBar.getWater_resistance();
            if (judgement_velocity < 0) judgement_velocity = 0;
        } else {
            judgement_velocity += modeTwoBar.getWater_resistance();
            if (judgement_velocity > 0) judgement_velocity = 0;
        }
    }

    private void reduceV() {
        fish_velocity -= modeTwoBar.getLoosening_loss();
    }

    private void addV(boolean add) {
        if (add) fish_velocity += modeTwoBar.getPulling_strength();
        else fish_velocity -= modeTwoBar.getPulling_strength();
    }

    private void calibrate() {
        if (fish_position < 0) {
            fish_position = 0;
            fish_velocity = 0;
        }
        if (fish_position + modeTwoBar.getFish_icon_width() > modeTwoBar.getBar_effective_width()) {
            fish_position = modeTwoBar.getBar_effective_width() - modeTwoBar.getFish_icon_width();
            fish_velocity = 0;
        }
        if (judgement_position < 0) {
            judgement_position = 0;
            judgement_velocity = 0;
        }
        if (judgement_position + modeTwoBar.getJudgement_area_width() > modeTwoBar.getBar_effective_width()) {
            judgement_position = modeTwoBar.getBar_effective_width() - modeTwoBar.getJudgement_area_width();
            judgement_velocity = 0;
        }
    }

    @Override
    public boolean isSuccess() {
        return success;
    }
}
