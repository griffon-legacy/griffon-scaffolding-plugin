package org.codehaus.griffon.cli.shell.command;

import org.codehaus.griffon.cli.shell.AbstractGriffonCommand;
import org.codehaus.griffon.cli.shell.Command;
import org.codehaus.griffon.cli.shell.Argument;
import org.codehaus.griffon.cli.shell.Option;

@Command(scope = "domain",
        name = "create-command-object",
        description = "Creates a new command object class")
public class CreateCommandObjectCommand extends AbstractGriffonCommand {
    @Argument(index = 0,
            name = "name",
            description = "The name of the command object to be created.",
            required = false)
    private String name;

    @Option(name = "--skip-package-prompt",
            description = "Skips the usage of the application's default package if the name of the class is not fully qualified.",
            required = false)
    private boolean skipPackagePrompt = false;

    @Option(name = "--file-type",
            description = "Source file type.",
            required = false)
    private String fileType = "groovy";

    @Option(name = "--archetype",
            description = "Archetype to be searched for templates.",
            required = false)
    private String archetype = "default";
}