package com.github.tezvn.starpvp.core.commands.main;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.commands.AbstractCommand;
import com.github.tezvn.starpvp.core.commands.main.arguments.StarPVPArgument;
import com.github.tezvn.starpvp.core.utils.ClassFinder;
import com.google.common.collect.Lists;

import java.util.Objects;

public class StarPVPCommand extends AbstractCommand<SPPlugin> {
    public StarPVPCommand(SPPlugin plugin) {
        super(plugin, "starpvp", "Main command of StarPVP", "/starpvp", Lists.newArrayList("sp", "spvp"));
        registerArguments(new ClassFinder(plugin).setPackage(StarPVPArgument.class).removeClass(StarPVPArgument.class).find().stream()
                .map(c -> {
                    try {
                        return c.getConstructor(SPPlugin.class).newInstance(plugin);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error while registering argument '" + c.getSimpleName() + "' for command '" + getName() + "'");
                        return null;
                    }
                }).filter(Objects::nonNull).distinct().toArray(CommandArgument[]::new));
    }

}
