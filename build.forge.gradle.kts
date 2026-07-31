plugins {
	id("mod-platform")
	id("net.neoforged.moddev.legacyforge")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed <= "1.16.5") {
		replace("dev.architectury", "me.shedaniel.architectury")
	}
	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			forgeLikeVersionRange = prop("deps.minecraft")
		}
		required("forge") {
			forgeLikeVersionRange.set("[1,)")
		}
		required("architectury") {
			slug("architectury-api")
			forgeLikeVersionRange = "[${prop("deps.architectury")},)"
		}
	}
}

legacyForge {
	version = "${prop("deps.minecraft")}-${prop("deps.forge")}"

	validateAccessTransformers = true

	accessTransformers.from(
		rootProject.file("src/main/resources/aw/${sc.current.version}.cfg")
	)

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "Forge Client (${sc.current.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "Forge Server (${sc.current.version})"
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
}

repositories {
	mavenCentral()
	strictMaven("https://maven.architectury.dev/", "dev.architectury", "me.shedaniel") { name = "Architectury" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

dependencies {
	val architecturyGroup = if (sc.current.parsed <= "1.16.5") "me.shedaniel" else "dev.architectury"
	implementation("$architecturyGroup:architectury-forge:${prop("deps.architectury")}")
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
