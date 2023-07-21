package com.github.tezvn.starpvp.core.commands.rank;

import com.github.tezvn.starpvp.api.SPPlugin;
import com.github.tezvn.starpvp.core.commands.AbstractCommand;
import com.github.tezvn.starpvp.core.commands.elo.arguments.EloArgument;
import com.github.tezvn.starpvp.core.commands.rank.arguments.DemoteArgument;
import com.github.tezvn.starpvp.core.commands.rank.arguments.PromoteArgument;
import com.github.tezvn.starpvp.core.commands.rank.arguments.RankArgument;
import com.github.tezvn.starpvp.core.commands.rank.arguments.SetArgument;
import com.github.tezvn.starpvp.core.utils.ClassFinder;

import java.util.Arrays;
import java.util.Objects;

public class RankCommand extends AbstractCommand<SPPlugin> {

    public RankCommand(SPPlugin plugin) {
        super(plugin, "rank", "Rank command", "rank", Arrays.asList("r", "sr"));
        registerArguments(new ClassFinder(plugin).setPackage(RankArgument.class).removeClass(RankArgument.class).find().stream().map(c -> {
            try {
                return c.getConstructor(SPPlugin.class).newInstance(plugin);
            }catch (Exception e) {
                plugin.getLogger().warning("Error while registering argument '" + c.getSimpleName() + "' for command '" + getName() + "'");
                return null;
            }
        }).filter(Objects::nonNull).toList().toArray(new CommandArgument[0]));
    }

}
