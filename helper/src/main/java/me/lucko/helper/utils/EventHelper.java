package me.lucko.helper.utils;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class EventHelper {

    public static Optional<Player> getPlayerDamager(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) e.getDamager();

            if ((projectile.getShooter() != null) && ((projectile.getShooter() instanceof Player)))
                return Optional.of((Player) projectile.getShooter());
        }
        else if (e.getDamager() instanceof Player)
            return Optional.of((Player) e.getDamager());

        return Optional.empty();
    }
}