package com.flyaway.chatmanager.managers;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerTracker {

    private final Map<UUID, Player> players = new ConcurrentHashMap<>();

    public void add(Player player) {
        players.put(player.getUniqueId(), player);
    }

    public void remove(Player player) {
        players.remove(player.getUniqueId());
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public Set<String> getPlayerNames() {
        return players.values()
                .stream()
                .map(Player::getName)
                .collect(Collectors.toSet());
    }
}
