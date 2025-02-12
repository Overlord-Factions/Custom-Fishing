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

package net.momirealms.customfishing.integration.quest;

import net.momirealms.customfishing.api.event.FishResultEvent;
import net.momirealms.customfishing.fishing.FishResult;
import net.momirealms.customfishing.util.AdventureUtils;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.VariableNumber;
import org.betonquest.betonquest.api.CountingObjective;
import org.betonquest.betonquest.api.profiles.OnlineProfile;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.betonquest.betonquest.utils.location.CompoundLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.HashSet;

public class NewBetonQuestCFQuest extends CountingObjective implements Listener {

    private final CompoundLocation playerLocation;
    private final VariableNumber rangeVar;
    private final HashSet<String> loot_ids;

    public NewBetonQuestCFQuest(Instruction instruction) throws InstructionParseException {
        super(instruction, "loot_to_fish");
        loot_ids = new HashSet<>();
        Collections.addAll(loot_ids, instruction.getArray());
        targetAmount = instruction.getVarNum();
        preCheckAmountNotLessThanOne(targetAmount);
        final String pack = instruction.getPackage().getQuestPath();
        final String loc = instruction.getOptional("playerLocation");
        final String range = instruction.getOptional("range");
        if (loc != null && range != null) {
            playerLocation = new CompoundLocation(pack, loc);
            rangeVar = new VariableNumber(pack, range);
        } else {
            playerLocation = null;
            rangeVar = null;
        }
    }

    public static void register() {
        BetonQuest.getInstance().registerObjectives("customfishing", NewBetonQuestCFQuest.class);
    }

    @EventHandler
    public void onFish(FishResultEvent event) {
        if (event.getResult() != FishResult.FAILURE) {
            OnlineProfile onlineProfile = PlayerConverter.getID(event.getPlayer());
            if (!containsPlayer(onlineProfile)) {
                return;
            }
            if (isInvalidLocation(event, onlineProfile)) {
                return;
            }
            if (this.loot_ids.contains(event.getLootID()) && this.checkConditions(onlineProfile)) {
                getCountingData(onlineProfile).progress(event.isDouble() ? 1 : 2);
                completeIfDoneOrNotify(onlineProfile);
            }
        }
    }

    private boolean isInvalidLocation(FishResultEvent event, final Profile profile) {
        if (playerLocation == null || rangeVar == null) {
            return false;
        }

        final Location targetLocation;
        try {
            targetLocation = playerLocation.getLocation(profile);
        } catch (final org.betonquest.betonquest.exceptions.QuestRuntimeException e) {
            AdventureUtils.consoleMessage(e.getMessage());
            return true;
        }
        final int range = rangeVar.getInt(profile);
        final Location playerLoc = event.getPlayer().getLocation();
        return !playerLoc.getWorld().equals(targetLocation.getWorld()) || targetLocation.distanceSquared(playerLoc) > range * range;
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, BetonQuest.getInstance());
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }
}
