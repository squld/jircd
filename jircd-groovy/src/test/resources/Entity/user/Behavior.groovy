package user;

static def initialize(shell, entity) {
	try {
		shell.evaluate("${entity.type}.${entity.id}.Behavior.initialize(shell, entity);", "${entity.type}/${entity.id}/Behavior.groovy");
	} catch (groovy.lang.MissingPropertyException e) {
		shell.out.println("${entity.type}/${entity.id}/Behavior.groovy is nothing");
	}
}
