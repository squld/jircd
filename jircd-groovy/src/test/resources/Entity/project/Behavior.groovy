package project;

static def initialize(shell, entity) {
	try {
		shell.evaluate("${entity.type}.${entity.projectName}.Behavior.initialize(shell, entity);", "${entity.type}/${entity.projectName}/Behavior.groovy");
	} catch (groovy.lang.MissingPropertyException e) {
		shell.out.println("${entity.type}/${entity.projectName}/Behavior.groovy is nothing");
	}
}
