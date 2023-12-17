package net.velinquish.cosmicmeteors.commands;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.mineacademy.fo.annotation.AutoRegister;
import org.mineacademy.fo.command.DebugCommand;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;

@AutoRegister
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MeteorCommandGroup extends SimpleCommandGroup {

    @Getter(value = AccessLevel.PRIVATE)
    private static final MeteorCommandGroup instance = new MeteorCommandGroup();


    @Override
    protected ChatColor getTheme() {
        return ChatColor.DARK_RED;
    }

    @Override
    protected String getCredits() {
        return "";
    }

    @Override
    protected void registerSubcommands() {
        registerSubcommand(new SpawnCommand());
        registerSubcommand(new SetCommand());

        registerSubcommand(new ReloadCommand("meteor.reload"));
        registerSubcommand(new DebugCommand("meteor.debug"));
    }
}
