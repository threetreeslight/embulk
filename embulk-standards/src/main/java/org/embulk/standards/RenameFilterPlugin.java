package org.embulk.standards;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;

import java.util.List;
import java.util.Locale;
import java.util.Map;


public class RenameFilterPlugin
        implements FilterPlugin
{
    public interface PluginTask
            extends Task
    {
        @Config("columns")
        @ConfigDefault("{}")
        Map<String, String> getRenameMap();

        @Config("rules")
        @ConfigDefault("[]")
        List<ConfigSource> getRulesList();
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema,
                            FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        Map<String, String> renameMap = task.getRenameMap();
        List<ConfigSource> rulesList = task.getRulesList();

        // Check if the given column in "columns" exists or not.
        for (String columnName : renameMap.keySet()) {
            inputSchema.lookupColumn(columnName); // throws SchemaConfigException
        }

        // Rename by "columns": to be applied before "rules".
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            String name = column.getName();
            if (renameMap.containsKey(name)) {
                name = renameMap.get(name);
            }
            builder.add(name, column.getType());
        }
        Schema intermediateSchema = builder.build();

        // Rename by "rules".
        Schema outputSchema = intermediateSchema;
        for (ConfigSource rule : rulesList) {
            outputSchema = applyRule(rule, intermediateSchema);
            intermediateSchema = outputSchema;
        }

        control.run(task.dump(), outputSchema);
    }

    @Override
    public PageOutput open(TaskSource taskSource, Schema inputSchema,
                           Schema outputSchema, PageOutput output)
    {
        return output;
    }

    // Extending Task is required to be deserialized with ConfigSource.loadConfig()
    // although this Rule is not really a Task.
    // TODO(dmikurube): Revisit this to consider how not to extend Task for this.
    private interface Rule
            extends Task
    {
        @Config("rule")
        String getRule();
    }

    private interface TruncateRule
            extends Rule {
        @Config("max_length")
        @ConfigDefault("128")
        int getMaxLength();
    }

    private Schema applyRule(ConfigSource ruleConfig, Schema inputSchema) throws ConfigException
    {
        Rule rule = ruleConfig.loadConfig(Rule.class);
        switch (rule.getRule()) {
        case "lower_to_upper":
            return applyLowerToUpperRule(inputSchema);
        case "truncate":
            return applyTruncateRule(inputSchema, ruleConfig.loadConfig(TruncateRule.class));
        case "upper_to_lower":
            return applyUpperToLowerRule(inputSchema);
        default:
            throw new ConfigException("Renaming rule \"" +rule+ "\" is unknown");
        }
    }

    private Schema applyLowerToUpperRule(Schema inputSchema) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().toUpperCase(Locale.ENGLISH), column.getType());
        }
        return builder.build();
    }

    private Schema applyTruncateRule(Schema inputSchema, TruncateRule rule) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().substring(0, rule.getMaxLength()), column.getType());
        }
        return builder.build();
    }

    private Schema applyUpperToLowerRule(Schema inputSchema) {
        Schema.Builder builder = Schema.builder();
        for (Column column : inputSchema.getColumns()) {
            builder.add(column.getName().toLowerCase(Locale.ENGLISH), column.getType());
        }
        return builder.build();
    }
}
