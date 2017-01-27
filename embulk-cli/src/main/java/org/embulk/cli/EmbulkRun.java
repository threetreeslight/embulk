package org.embulk.cli;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EmbulkRun
{
    public static void main(String[] args)
    {
        (new EmbulkRun()).run(ImmutableList.copyOf(args));
    }

    public void run(List<String> argv)
    {
        // # reset context class loader set by org.jruby.Main.main to nil. embulk manages
        // # multiple classloaders. default classloader should be Plugin.class.getClassloader().
        // JRuby: java.lang.Thread.current_thread.set_context_class_loader(nil)
        Thread.currentThread().setContextClassLoader(null);

        // TODO:
        // require 'embulk/version'
        String version = "0.X.X";

        // i = argv.find_index {|arg| arg !~ /^\-/ }
        // unless i
        //   if argv.include?('--version')
        //     puts "embulk #{Embulk::VERSION}"
        //     system_exit_success
        //   end
        //   usage nil
        // end
        // subcmd = argv.slice!(i).to_sym
        Optional<String> subcmd = Optional.absent();
        for (String arg : argv) {
            if (!arg.startsWith("-")) {
                subcmd = Optional.of(arg);
            }
        }
        if (!subcmd.isPresent()) {
            if (argv.contains("--version")) {
                System.out.printf("embulk %s\n", version);
                exitSystemSuccessfully();
            }
        }

        // require 'java'
        // require 'optparse'
        // op = OptionParser.new
        // op.version = Embulk::VERSION
        EmbulkOptions op = new EmbulkOptions();
        EmbulkHelpFormatter formatter = new EmbulkHelpFormatter();
        Range<Integer> args = Range.closed(1, 1);
        ImmutableMap.Builder<String, Object> optionsBuilder = ImmutableMap.builder();
        String usage = "";

        // puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: Embulk v#{Embulk::VERSION}"

        // plugin_paths = []
        // load_paths = []
        // classpaths = []
        // classpath_separator = java.io.File.pathSeparator

        // options = {
        //   system_config: {}
        // }

        switch (subcmd.or("")) {
        case "run":
            // op.banner = "Usage: embulk run <config.yml>"
            usage = "embulk run <config.yml>";
            // op.separator "  Options:"
            op.addSeparator("  Options:");
            // op.on('-r', '--resume-state PATH', 'Path to a file to write or read resume state') do |path|
            //   options[:resume_state_path] = path
            // end
            op.addOption(Option.builder("r")
                         .longOpt("resume-state")
                         .hasArg()
                         .argName("PATH")
                         .desc("Path to a file to write or read resume state")
                         .build());
            // op.on('-o', '--output PATH', '(deprecated)') do |path|
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: Run with -o option is deprecated. Please use -c option instead. For example,"
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}:   $ embulk run config.yml -c diff.yml"
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: This -c option stores only diff of the next configuration."
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: The diff will be merged to the original config.yml file."
            //   STDERR.puts "#{Time.now.strftime("%Y-%m-%d %H:%M:%S.%3N %z")}: "
            //   options[:next_config_output_path] = path
            // end
            op.addOption(Option.builder("o")
                         .longOpt("output")
                         .hasArg()
                         .argName("PATH")
                         .desc("(deprecated)")
                         .build());
            // op.on('-c', '--config-diff PATH', 'Path to a file to read & write the next configuration diff') do |path|
            //   options[:next_config_diff_path] = path
            // end
            op.addOption(Option.builder("c")
                         .longOpt("config-diff")
                         .hasArg()
                         .argName("PATH")
                         .desc("Path to a file to read & write the next configuration diff")
                         .build());
            // plugin_load_ops.call
            addPluginLoadOps(op);
            // java_embed_ops.call
            addJavaEmbedOps(op);
            // args = 1..1
            args = Range.closed(1, 1);
            break;
        case "cleanup":
            // op.banner = "Usage: embulk cleanup <config.yml>"
            // op.separator "  Options:"
            // op.on('-r', '--resume-state PATH', 'Path to a file to cleanup resume state') do |path|
            //   options[:resume_state_path] = path
            // end
            // plugin_load_ops.call
            // java_embed_ops.call
            // args = 1..1
            args = Range.closed(1, 1);
            break;
        case "preview":
            // op.banner = "Usage: embulk preview <config.yml>"
            // op.separator "  Options:"
            // op.on('-G', '--vertical', "Use vertical output format", TrueClass) do |b|
            //   options[:format] = "vertical"
            // end
            // plugin_load_ops.call
            // java_embed_ops.call
            // args = 1..1
            args = Range.closed(1, 1);
            break;
        case "guess":
            // op.banner = "Usage: embulk guess <partial-config.yml>"
            // op.separator "  Options:"
            // op.on('-o', '--output PATH', 'Path to a file to write the guessed configuration') do |path|
            //   options[:next_config_output_path] = path
            // end
            // op.on('-g', '--guess NAMES', "Comma-separated list of guess plugin names") do |names|
            //   (options[:system_config][:guess_plugins] ||= []).concat names.split(",")  # TODO
            // end
            // plugin_load_ops.call
            // java_embed_ops.call
            // args = 1..1
            args = Range.closed(1, 1);
            break;
        case "mkbundle":
            // op.banner = "Usage: embulk mkbundle <directory> [--path PATH]"
            // op.separator "  Options:"
            // op.on('--path PATH', 'Relative path from <directory> for the location to install gems to (e.g. --path shared/bundle).') do |path|
            //   options[:bundle_path] = path
            // end
            // op.separator <<-EOF
/*

  "mkbundle" creates a new a plugin bundle directory. You can install
  plugins (gems) to the directory instead of ~/.embulk.

  See generated <directory>/Gemfile to install plugins to the directory.
  Use -b, --bundle BUNDLE_DIR option to use it:

    $ embulk mkbundle ./dir                # create bundle directory
    $ (cd dir && vi Gemfile && embulk bundle)   # update plugin list
    $ embulk guess -b ./dir ...            # guess using bundled plugins
    $ embulk run   -b ./dir ...            # run using bundled plugins

*/
            // EOF
            // args = 1..1
            args = Range.closed(1, 1);
            break;
        case "bundle":
            // if argv[0] == 'new'
            //   usage nil if argv.length != 2
            //   new_bundle(argv[1], nil)
            //   STDERR.puts "'embulk bundle new' is deprecated. This will be removed in future release. Please use 'embulk mkbundle' instead."
            // else
            //   run_bundler(argv)
            // end
            // system_exit_success
            break;
        case "gem":
            // require 'rubygems/gem_runner'
            // Gem::GemRunner.new.run argv
            // system_exit_success
            break;
        case "new":
            // op.banner = "Usage: embulk new <category> <name>" + %[
/*
categories:
    ruby-input                 Ruby record input plugin    (like "mysql")
    ruby-output                Ruby record output plugin   (like "mysql")
    ruby-filter                Ruby record filter plugin   (like "add-hostname")
    #ruby-file-input           Ruby file input plugin      (like "ftp")          # not implemented yet [#21]
    #ruby-file-output          Ruby file output plugin     (like "ftp")          # not implemented yet [#22]
    ruby-parser                Ruby file parser plugin     (like "csv")
    ruby-formatter             Ruby file formatter plugin  (like "csv")
    #ruby-decoder              Ruby file decoder plugin    (like "gzip")         # not implemented yet [#31]
    #ruby-encoder              Ruby file encoder plugin    (like "gzip")         # not implemented yet [#32]
    java-input                 Java record input plugin    (like "mysql")
    java-output                Java record output plugin   (like "mysql")
    java-filter                Java record filter plugin   (like "add-hostname")
    java-file-input            Java file input plugin      (like "ftp")
    java-file-output           Java file output plugin     (like "ftp")
    java-parser                Java file parser plugin     (like "csv")
    java-formatter             Java file formatter plugin  (like "csv")
    java-decoder               Java file decoder plugin    (like "gzip")
    java-encoder               Java file encoder plugin    (like "gzip")

examples:
    new ruby-output hbase
    new ruby-filter int-to-string
]
*/
            // args = 2..2
            args = Range.closed(2, 2);
            break;
        case "migrate":
            // op.banner = "Usage: embulk migrate <directory>"
            // args = 1..1
            args = Range.closed(1, 1);
            break;
        case "selfupdate":
            // NOTE: A long version "--force" is added from the original Ruby version.
            // op.on('-f', "Skip corruption check", TrueClass) do |b|
            //   options[:force] = true
            // end
            // args = 0..1
            args = Range.closed(0, 1);
            break;
        case "example":
            // args = 0..1
            args = Range.closed(0, 1);
            break;
        case "exec":
            // exec(*argv)
            // exit 127
            break;
        case "irb":
            // require 'irb'
            // IRB.start
            // system_exit_success
            break;
        default:
            // usage "Unknown subcommand #{subcmd.to_s.dump}."
        }


        //  begin
        //    op.parse!(argv)
        //    unless args.include?(argv.length)
        //      usage_op op, nil
        //    end
        //  rescue => e
        //    usage_op op, e.to_s
        //  end
        List<String> argsSpecified = null;
        List<Option> optionsSpecified = null;
        try {
            CommandLine command = new DefaultParser().parse(op, Iterables.toArray(argv, String.class));
            argsSpecified = command.getArgList();
            optionsSpecified = ImmutableList.copyOf(command.getOptions());
        } catch (ParseException ex) {
            formatter.printHelp(usage, "", op, "", false);
            abortSystem(Optional.of(ex.toString()));
        }
        if (!args.contains(argsSpecified.size())) {
            formatter.printHelp(usage, "", op, "", false);
            abortSystem(Optional.<String>absent());
        }

        for (Option o : optionsSpecified) {
            if ("X".equals(o.getOpt())) {
            }

            switch (o.getLongOpt()) {
            case "resume-state":
                optionsBuilder.put("resume_state_path", o.getValue());
                break;
            case "output":
                warnOutputDeprecated();
                optionsBuilder.put("next_config_output_path", o.getValue());
                break;
            case "config-diff":
                break;
            case "vertical":
                break;
            case "guess":
                break;
            case "path":
                break;
            case "force":
                break;
            case "log":
                break;
            case "log-level":
                break;
            case "load":
                break;
            case "load-path":
                break;
            case "classpath":
                break;
            case "bundle":
                break;
            }
        }

        switch (subcmd.or("")) {
        case "example":
            // require 'embulk/command/embulk_example'
            // path = ARGV[0] || "embulk-example"
            // puts "Creating #{path} directory..."
            // Embulk.create_example(path)
            // puts ""
            // puts "Run following subcommands to try embulk:"
            // puts ""
            // puts "   1. embulk guess #{File.join(path, 'seed.yml')} -o config.yml"
            // puts "   2. embulk preview config.yml"
            // puts "   3. embulk run config.yml"
            // puts ""
            break;
        case "new":
            // lang_cate = ARGV[0]
            // name = ARGV[1]
            //
            // language, category = case lang_cate
            //   when "java-input"       then [:java, :input]
            //   when "java-output"      then [:java, :output]
            //   when "java-filter"      then [:java, :filter]
            //   when "java-file-input"  then [:java, :file_input]
            //   when "java-file-output" then [:java, :file_output]
            //   when "java-parser"      then [:java, :parser]
            //   when "java-formatter"   then [:java, :formatter]
            //   when "java-decoder"     then [:java, :decoder]
            //   when "java-encoder"     then [:java, :encoder]
            //   when "ruby-input"       then [:ruby, :input]
            //   when "ruby-output"      then [:ruby, :output]
            //   when "ruby-filter"      then [:ruby, :filter]
            //   when "ruby-file-input"  then raise "ruby-file-input is not implemented yet. See #21 on github." #[:ruby, :file_input]
            //   when "ruby-file-output" then raise "ruby-file-output is not implemented yet. See #22 on github." #[:ruby, :file_output]
            //   when "ruby-parser"      then [:ruby, :parser]
            //   when "ruby-formatter"   then [:ruby, :formatter]
            //   when "ruby-decoder"     then raise "ruby-decoder is not implemented yet. See #31 on github." #[:ruby, :decoder]
            //   when "ruby-encoder"     then raise "ruby-decoder is not implemented yet. See #32 on github." #[:ruby, :encoder]
            //   else
            //     usage_op op, "Unknown category '#{lang_cate}'"
            //   end
            //
            // require 'embulk/command/embulk_new_plugin'
            // Embulk.new_plugin(name, language, category)
            break;
        case "migrate":
            // path = ARGV[0]
            // require 'embulk/command/embulk_migrate_plugin'
            // Embulk.migrate_plugin(path)
            break;
        case "selfupdate":
            // require 'embulk/command/embulk_selfupdate'
            // options[:version] = ARGV[0]
            // Embulk.selfupdate(options)
            break;
        case "mkbundle":
            // new_bundle(argv[0], options[:bundle_path])
            break;
        default:
            // require 'json'
            //
            // # Gem::StubSpecification is an internal API that seems chainging often.
            // # Gem::Specification.add_spec is deprecated also. Therefore, here makes
            // # -L <path> option alias of -I <path>/lib by assuming that *.gemspec file
            // # always has require_paths = ["lib"].
            // load_paths = load_paths + plugin_paths.map {|path| File.join(path, "lib") }
            //
            // setup_load_paths(load_paths)
            // setup_classpaths(classpaths)
            //
            // # call setup after setup_classpaths to allow users to overwrite
            // # embulk classes
            // Embulk.setup(options.delete(:system_config))
            //
            // begin
            //   case subcmd
            //   when :guess
            //     puts "[ CALL] Embulk::Runner.guess:"
            //     puts "  #{argv[0]}"
            //     puts "  #{options}"
            //     puts "  #{Embulk::Runner}"
            //     puts ""
            //     Embulk::Runner.guess(argv[0], options)
            //   when :preview
            //     Embulk::Runner.preview(argv[0], options)
            //   when :run
            //     Embulk::Runner.run(argv[0], options)
            //   end
            // rescue => ex
            //   print_exception(ex)
            //   puts ""
            //   puts "Error: #{ex}"
            //   raise SystemExit.new(1, ex.to_s)
            // end
        }
    }

    private void warnOutputDeprecated()
    {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS Z");
        String now = DateTime.now().toString(formatter);
        System.err.println(now + ": Run with -o option is deprecated. Please use -c option instead. For example,");
        System.err.println(now + ": ");
        System.err.println(now + ":   $ embulk run config.yml -c diff.yml");
        System.err.println(now + ": ");
        System.err.println(now + ": This -c option stores only diff of the next configuration.");
        System.err.println(now + ": The diff will be merged to the original config.yml file.");
        System.err.println(now + ": ");
    }

    private void usage(Optional<String> message)
    {
        System.err.println("Embulk v#{Embulk::VERSION}");
        System.err.println("Usage: embulk [-vm-options] <command> [--options]");
        System.err.println("Commands:");
        System.err.println("   mkbundle   <directory>                             # create a new plugin bundle environment.");
        System.err.println("   bundle     [directory]                             # update a plugin bundle environment.");
        System.err.println("   run        <config.yml>                            # run a bulk load transaction.");
        System.err.println("   cleanup    <config.yml>                            # cleanup resume state.");
        System.err.println("   preview    <config.yml>                            # dry-run the bulk load without output and show preview.");
        System.err.println("   guess      <partial-config.yml> -o <output.yml>    # guess missing parameters to create a complete configuration file.");
        System.err.println("   gem        <install | list | help>                 # install a plugin or show installed plugins.");
        System.err.println("                                                      # plugin path is #{ENV['GEM_HOME']}");
        System.err.println("   new        <category> <name>                       # generates new plugin template");
        System.err.println("   migrate    <path>                                  # modify plugin code to use the latest Embulk plugin API");
        System.err.println("   example    [path]                                  # creates an example config file and csv file to try embulk.");
        System.err.println("   selfupdate [version]                               # upgrades embulk to the latest released version or to the specified version.");
        System.err.println("");
        System.err.println("VM options:");
        System.err.println("   -J-O                             Disable JVM optimizations to speed up startup time (enabled by default if command is 'run')");
        System.err.println("   -J+O                             Enable JVM optimizations to speed up throughput");
        System.err.println("   -J...                            Set JVM options (use -J-help to see available options)");
        System.err.println("   -R...                            Set JRuby options (use -R--help to see available options)");
        System.err.println("");
        if (message.isPresent()) {
            abortSystem(Optional.of("Use `<command> --help` to see description of the commands."));
        } else {
            abortSystem(Optional.of("error: " + message.or("")));
        }
    }

    private void addJavaEmbedOps(EmbulkOptions embulkOptions)
    {
        //   op.separator ""
        embulkOptions.addSeparator("");
        //   op.separator "  Other options:"
        embulkOptions.addSeparator("  Other options:");
        //   op.on('-l', '--log PATH', 'Output log messages to a file (default: -)') do |path|
        //     options[:system_config][:log_path] = path
        //   end
        embulkOptions.addOption(Option.builder()  // "-l" is duplicated...
                                .longOpt("log")
                                .hasArg()
                                .argName("PATH")
                                .desc("Output log messages to a file (default: -)")
                                .build());
        //   op.on('-l', '--log-level LEVEL', 'Log level (error, warn, info, debug or trace)') do |level|
        //     options[:system_config][:log_level] = level
        //   end
        embulkOptions.addOption(Option.builder("l")
                                .longOpt("log-level")
                                .hasArg()
                                .argName("LEVEL")
                                .desc("Log level (error, warn, info, debug or trace)")
                                .build());
        //   op.on('-X KEY=VALUE', 'Add a performance system config') do |kv|
        //     k, v = kv.split('=', 2)
        //     v ||= "true"
        //     options[:system_config][k] = v
        //   end
        embulkOptions.addOption(Option.builder("X")
                                .hasArg()
                                .argName("KEY=VALUE")
                                .desc("Add a performance system config")
                                .build());
    }

    private void addPluginLoadOps(EmbulkOptions embulkOptions)
    {
        //   op.separator ""
        embulkOptions.addSeparator("");
        //   op.separator "  Plugin load options:"
        embulkOptions.addSeparator("  Plugin load options:");
        //   op.on('-L', '--load PATH', 'Add a local plugin path') do |plugin_path|
        //     plugin_paths << plugin_path
        //   end
        embulkOptions.addOption(Option.builder("L")
                                .longOpt("load")
                                .hasArg()
                                .argName("PATH")
                                .desc("Add a local plugin path")
                                .build());
        //   op.on('-I', '--load-path PATH', 'Add ruby script directory path ($LOAD_PATH)') do |load_path|
        //     load_paths << load_path
        //   end
        embulkOptions.addOption(Option.builder("I")
                                .longOpt("load-path")
                                .hasArg()
                                .argName("PATH")
                                .desc("Add ruby script directory path ($LOAD_PATH)")
                                .build());
        //   op.on('-C', '--classpath PATH', "Add java classpath separated by #{classpath_separator} (CLASSPATH)") do |classpath|
        //     classpaths.concat classpath.split(classpath_separator)
        //   end
        embulkOptions.addOption(Option.builder("C")
                                .longOpt("classpath")
                                .hasArg()
                                .argName("PATH")
                                .desc("Add java classpath separated by " + java.io.File.pathSeparator + " (CLASSPATH)")
                                .build());
        //   op.on('-b', '--bundle BUNDLE_DIR', 'Path to a Gemfile directory (create one using "embulk mkbundle" command)') do |path|
        //     # only for help message. implemented at lib/embulk/command/embulk_bundle.rb
        //   end
        embulkOptions.addOption(Option.builder("b")
                                .longOpt("bundle")
                                .hasArg()
                                .argName("BUNDLE_DIR")
                                .desc("Path to a Gemfile directory (create one using \"embulk mkbundle\" command)")
                                .build());
    }

    private void abortSystem(Optional<String> message)
    {
        if (message.isPresent()) {
            System.err.println(message);
        }
        System.exit(1);
    }

    private void exitSystemSuccessfully()
    {
        System.exit(0);
    }
}

class EmbulkSeparatorDummyOption extends Option
{
    public EmbulkSeparatorDummyOption(String separator)
    {
        super("_", "_");
        this.separator = separator;
    }

    public String getSeparator()
    {
        return this.separator;
    }

    private String separator;
}

class EmbulkOptions extends Options
{
    public EmbulkOptions()
    {
        this.optionsWithSeparators = new ArrayList<Option>();
    }

    public Options addSeparator(String separator)
    {
        this.optionsWithSeparators.add(new EmbulkSeparatorDummyOption(separator));
        return this;
    }

    @Override
    public Options addOption(Option opt)
    {
        this.optionsWithSeparators.add(opt);
        return super.addOption(opt);
    }

    public List<Option> getOptionsWithSeparators()
    {
        return ImmutableList.copyOf(this.optionsWithSeparators);
    }

    private List<Option> optionsWithSeparators;
}

class EmbulkHelpFormatter extends HelpFormatter
{
    public EmbulkHelpFormatter()
    {
        super();
        this.setLeftPadding(4);
        this.setSyntaxPrefix("Usage: ");
    }

    @Override
    protected StringBuffer renderOptions(StringBuffer sb, int width, Options options, int leftPad, int descPad)
    {
        if (!(options instanceof EmbulkOptions)) {
            return super.renderOptions(sb, width, options, leftPad, descPad);
        }

        EmbulkOptions eoptions = (EmbulkOptions)options;

        final String lpad = createPadding(leftPad);
        final String dpad = createPadding(descPad);

        // first create list containing only <lpad>-a,--aaa where
        // -a is opt and --aaa is long opt; in parallel look for
        // the longest opt string this list will be then used to
        // sort options ascending
        int max = 0;
        List<StringBuffer> prefixList = new ArrayList<StringBuffer>();

        List<Option> optList = eoptions.getOptionsWithSeparators();

        for (Option option : optList)
        {
            StringBuffer optBuf = new StringBuffer();

            if (option instanceof EmbulkSeparatorDummyOption) {
                optBuf.append(((EmbulkSeparatorDummyOption)option).getSeparator());
                prefixList.add(optBuf);
                continue;
            }

            if (option.getOpt() == null)
            {
                optBuf.append(lpad).append("    ").append(getLongOptPrefix()).append(option.getLongOpt());
            }
            else
            {
                optBuf.append(lpad).append(getOptPrefix()).append(option.getOpt());

                if (option.hasLongOpt())
                {
                    optBuf.append(", ").append(getLongOptPrefix()).append(option.getLongOpt());
                }
            }

            if (option.hasArg())
            {
                String argName = option.getArgName();
                if (argName != null && argName.length() == 0)
                {
                    // if the option has a blank argname
                    optBuf.append(' ');
                }
                else
                {
                    optBuf.append(option.hasLongOpt() ? getLongOptSeparator() : " ");
                    optBuf.append(argName != null ? option.getArgName() : getArgName());
                }
            }

            prefixList.add(optBuf);
            max = optBuf.length() > max ? optBuf.length() : max;
        }

        int x = 0;

        for (Iterator<Option> it = optList.iterator(); it.hasNext();)
        {
            Option option = it.next();

            if (option instanceof EmbulkSeparatorDummyOption) {
                String separator = prefixList.get(x++).toString();
                sb.append(separator);
                sb.append(getNewLine());
                continue;
            }

            StringBuilder optBuf = new StringBuilder(prefixList.get(x++).toString());

            if (optBuf.length() < max)
            {
                optBuf.append(createPadding(max - optBuf.length()));
            }

            optBuf.append(dpad);

            int nextLineTabStop = max + descPad;

            if (option.getDescription() != null)
            {
                optBuf.append(option.getDescription());
            }

            renderWrappedText(sb, width, nextLineTabStop, optBuf.toString());

            if (it.hasNext())
            {
                sb.append(getNewLine());
            }
        }

        return sb;
    }
}

/*
  def self.run(argv)
    puts "[START] embulk_run.rb: Embulk.run:"
    argv.each do |arg|
      print "  <#{arg}>"
    end
    puts ""
    puts "  ..."
    puts ""


  // The method is no longer called...
  def self.default_gem_home
    if RUBY_PLATFORM =~ /java/i
      user_home = java.lang.System.properties["user.home"]
    end
    user_home ||= ENV['HOME']
    unless user_home
      raise "HOME environment variable is not set."
    end
    File.expand_path File.join(user_home, '.embulk', Gem.ruby_engine, RbConfig::CONFIG['ruby_version'])
  end
*/
