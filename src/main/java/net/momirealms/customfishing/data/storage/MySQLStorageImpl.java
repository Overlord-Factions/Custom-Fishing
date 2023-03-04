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

package net.momirealms.customfishing.data.storage;

import net.momirealms.customfishing.CustomFishing;
import net.momirealms.customfishing.data.PlayerBagData;
import net.momirealms.customfishing.data.PlayerSellData;
import net.momirealms.customfishing.util.AdventureUtil;
import net.momirealms.customfishing.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.UUID;

public class MySQLStorageImpl implements DataStorageInterface {

    private final SqlConnection sqlConnection;
    private final CustomFishing plugin;

    public MySQLStorageImpl(CustomFishing plugin) {
        this.plugin = plugin;
        this.sqlConnection = new SqlConnection(this);
    }

    @Override
    public void initialize() {
        sqlConnection.createNewHikariConfiguration();
        createTableIfNotExist(sqlConnection.getTablePrefix() + "_fishingbag", SqlConstants.SQL_CREATE_BAG_TABLE);
        createTableIfNotExist(sqlConnection.getTablePrefix() + "_selldata", SqlConstants.SQL_CREATE_SELL_TABLE);
    }

    @Override
    public void disable() {
        sqlConnection.close();
    }

    @Override
    public Inventory loadBagData(OfflinePlayer player, boolean force) {
        Inventory inventory = null;
        String sql = String.format(SqlConstants.SQL_SELECT_BAG_BY_UUID, sqlConnection.getTablePrefix() + "_" + "fishingbag");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int version = rs.getInt(2);
                if (!force && version != 0) {
                    statement.close();
                    connection.close();
                    return null;
                }
                int size = rs.getInt(3);
                String contents = rs.getString(4);
                ItemStack[] itemStacks = InventoryUtil.getInventoryItems(contents);
                inventory = Bukkit.createInventory(null, size, "{CustomFishing_Bag_" + player.getName() + "}");
                if (itemStacks != null) inventory.setContents(itemStacks);
                lockData(player.getUniqueId(), "fishingbag");
            }
            else {
                inventory = Bukkit.createInventory(null, 9, "{CustomFishing_Bag_" + player.getName() + "}");
                insertBagData(player.getUniqueId(), InventoryUtil.toBase64(inventory.getContents()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inventory;
    }

    @Override
    public void saveBagData(PlayerBagData playerBagData, boolean unlock) {
        UUID uuid = playerBagData.getPlayer().getUniqueId();
        Inventory inventory = playerBagData.getInventory();
        String contents = InventoryUtil.toBase64(inventory.getContents());
        if (contents == null) contents = "";
        updateBagData(uuid, inventory.getSize(), contents, unlock);
    }

    /**
     * Whether the data is loaded
     * @param player player
     * @return success or not
     */
    @Override
    public PlayerSellData loadSellData(Player player, boolean force) {
        PlayerSellData playerSellData = null;
        String sql = String.format(SqlConstants.SQL_SELECT_SELL_BY_UUID, sqlConnection.getTablePrefix() + "_" + "selldata");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, player.getUniqueId().toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int version = rs.getInt(2);
                if (!force && version != 0) {
                    statement.close();
                    connection.close();
                    return null;
                }
                int date = rs.getInt(3);
                int money = rs.getInt(4);
                playerSellData = new PlayerSellData(money, date);
                lockData(player.getUniqueId(), "selldata");
            }
            else {
                Calendar calendar = Calendar.getInstance();
                playerSellData = new PlayerSellData(calendar.get(Calendar.MONTH) * 100 + calendar.get(Calendar.DATE), 0);
                insertSellData(player.getUniqueId(), playerSellData.getDate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playerSellData;
    }

    @Override
    public void saveSellData(UUID uuid, PlayerSellData playerSellData, boolean unlock) {
        updateSellData(uuid, playerSellData.getDate(), (int) playerSellData.getMoney(), unlock);
    }

    private void createTableIfNotExist(String table, String sqlStat) {
        String sql = String.format(sqlStat, table);
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("[CustomFishing] Failed to create table");
        }
    }

    private void insertBagData(UUID uuid, String contents) {
        String sql = String.format(SqlConstants.SQL_INSERT_BAG, sqlConnection.getTablePrefix() + "_" + "fishingbag");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, 1);
            statement.setInt(3, 9);
            statement.setString(4, contents);
            statement.executeUpdate();
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("[CustomFishing] Failed to insert data for " + uuid);
        }
    }

    private void insertSellData(UUID uuid, int date) {
        String sql = String.format(SqlConstants.SQL_INSERT_SELL, sqlConnection.getTablePrefix() + "_" + "selldata");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setInt(2, 1);
            statement.setInt(3, date);
            statement.setInt(4, 0);
            statement.executeUpdate();
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("[CustomFishing] Failed to insert data for " + uuid);
        }
    }

    private void updateBagData(UUID uuid, int size, String contents, boolean unlock) {
        String sql = String.format(SqlConstants.SQL_UPDATE_BAG_BY_UUID, sqlConnection.getTablePrefix() + "_" + "fishingbag");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, unlock ? 0 : 1);
            statement.setInt(2, size);
            statement.setString(3, contents);
            statement.setString(4, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("[CustomFishing] Failed to update data for " + uuid);
        }
    }

    private void updateSellData(UUID uuid, int date, int money, boolean unlock) {
        String sql = String.format(SqlConstants.SQL_UPDATE_SELL_BY_UUID, sqlConnection.getTablePrefix() + "_" + "selldata");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, unlock ? 0 : 1);
            statement.setInt(2, date);
            statement.setInt(3, money);
            statement.setString(4, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("[CustomFishing] Failed to update data for " + uuid);
        }
    }

    public void migrate() {
        String sql_1 = String.format(SqlConstants.SQL_ALTER_TABLE, sqlConnection.getTablePrefix() + "_" + "fishingbag");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql_1)) {
            statement.executeUpdate();
            AdventureUtil.consoleMessage("<green>[CustomFishing] 1/2 tables updated");
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("<red>[CustomFishing] Failed to migrate data");
        }
        String sql_2 = String.format(SqlConstants.SQL_ALTER_TABLE, sqlConnection.getTablePrefix() + "_" + "selldata");
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql_2)) {
            statement.executeUpdate();
            AdventureUtil.consoleMessage("<green>[CustomFishing] 2/2 tables updated");
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("<red>[CustomFishing] Failed to migrate data");
        }
    }

    public void lockData(UUID uuid, String table_suffix) {
        String sql = String.format(SqlConstants.SQL_LOCK_BY_UUID, sqlConnection.getTablePrefix() + "_" + table_suffix);
        try (Connection connection = sqlConnection.getConnectionAndCheck(); PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            AdventureUtil.consoleMessage("<red>[CustomFishing] Failed to lock data for " + uuid);
        }
    }
}
