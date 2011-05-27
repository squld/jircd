static def initialize(shell, entity) {
	shell.evaluate("${entity.type}.Behavior.initialize(shell, entity);", "${entity.type}/Behavior.groovy");
}

static def finalize(shell, entity) {
	shell.evaluate("${entity.type}.Behavior.finalize(shell, entity);", "${entity.type}/Behavior.groovy");
}

static def loadPlugins(shell, entity) {
	shell.evaluate("${entity.type}.Behavior.loadPlugins(shell, entity);", "${entity.type}/Behavior.groovy");
}
