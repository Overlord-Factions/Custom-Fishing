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

package net.momirealms.customfishing.integration.skill;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.experience.EXPSource;
import net.Indyuce.mmocore.experience.Profession;
import net.Indyuce.mmocore.manager.data.PlayerDataManager;
import net.momirealms.customfishing.integration.SkillInterface;
import org.bukkit.entity.Player;

public class MMOCoreImpl implements SkillInterface {

    private final Profession profession;
    private final PlayerDataManager playerDataManager;

    public MMOCoreImpl(String name) {
        profession = MMOCore.plugin.professionManager.get(name);
        playerDataManager = MMOCore.plugin.dataProvider.getDataManager();
    }

    @Override
    public void addXp(Player player, double amount) {
        if (profession != null) {
            profession.giveExperience(playerDataManager.get(player), amount, null ,EXPSource.OTHER);
        }
    }

    @Override
    public int getLevel(Player player) {
        return playerDataManager.get(player).getLevel();
    }
}