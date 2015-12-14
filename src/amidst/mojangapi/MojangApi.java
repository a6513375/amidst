package amidst.mojangapi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import amidst.documentation.ThreadSafe;
import amidst.mojangapi.file.FilenameFactory;
import amidst.mojangapi.file.directory.DotMinecraftDirectory;
import amidst.mojangapi.file.directory.ProfileDirectory;
import amidst.mojangapi.file.directory.SaveDirectory;
import amidst.mojangapi.file.directory.VersionDirectory;
import amidst.mojangapi.file.json.versionlist.VersionListJson;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.minecraftinterface.local.LocalMinecraftInterfaceBuilder.LocalMinecraftInterfaceCreationException;
import amidst.mojangapi.world.World;
import amidst.mojangapi.world.WorldBuilder;
import amidst.mojangapi.world.WorldSeed;
import amidst.mojangapi.world.WorldType;

@ThreadSafe
public class MojangApi {
	private static final String UNKNOWN_VERSION_ID = "unknown";

	private final WorldBuilder worldBuilder;
	private final DotMinecraftDirectory dotMinecraftDirectory;
	private final VersionListJson versionList;
	private final File preferedJson;

	private volatile ProfileDirectory profileDirectory;
	private volatile MinecraftInterface minecraftInterface;

	public MojangApi(WorldBuilder worldBuilder,
			DotMinecraftDirectory dotMinecraftDirectory,
			VersionListJson versionList, File preferedJson) {
		this.worldBuilder = worldBuilder;
		this.dotMinecraftDirectory = dotMinecraftDirectory;
		this.versionList = versionList;
		this.preferedJson = preferedJson;
	}

	public DotMinecraftDirectory getDotMinecraftDirectory() {
		return dotMinecraftDirectory;
	}

	public VersionListJson getVersionList() {
		return versionList;
	}

	public void set(ProfileDirectory profileDirectory,
			VersionDirectory versionDirectory)
			throws LocalMinecraftInterfaceCreationException {
		this.profileDirectory = profileDirectory;
		if (versionDirectory != null) {
			try {
				this.minecraftInterface = versionDirectory
						.createLocalMinecraftInterface();
			} catch (LocalMinecraftInterfaceCreationException e) {
				this.minecraftInterface = null;
				throw e;
			}
		} else {
			this.minecraftInterface = null;
		}
	}

	public VersionDirectory createVersionDirectory(String versionId) {
		File versions = dotMinecraftDirectory.getVersions();
		File jar = FilenameFactory.getClientJarFile(versions, versionId);
		File json = FilenameFactory.getClientJsonFile(versions, versionId);
		return doCreateVersionDirectory(versionId, jar, json);
	}

	public VersionDirectory createVersionDirectory(File jar, File json) {
		return doCreateVersionDirectory(UNKNOWN_VERSION_ID, jar, json);
	}

	private VersionDirectory doCreateVersionDirectory(String versionId,
			File jar, File json) {
		if (preferedJson != null) {
			return new VersionDirectory(dotMinecraftDirectory, versionId, jar,
					preferedJson);
		} else {
			return new VersionDirectory(dotMinecraftDirectory, versionId, jar,
					json);
		}
	}

	public File getSaves() {
		ProfileDirectory profileDirectory = this.profileDirectory;
		if (profileDirectory != null) {
			return profileDirectory.getSaves();
		} else {
			return dotMinecraftDirectory.getSaves();
		}
	}

	public boolean canCreateWorld() {
		return minecraftInterface != null;
	}

	/**
	 * Due to the limitation of the minecraft interface, you can only work with
	 * one world at a time. Creating a new world will break all previously
	 * created world objects.
	 */
	public World createWorldFromSeed(WorldSeed seed, WorldType worldType) {
		MinecraftInterface minecraftInterface = this.minecraftInterface;
		if (minecraftInterface != null) {
			return worldBuilder.fromSeed(minecraftInterface, seed, worldType);
		} else {
			throw new IllegalStateException(
					"cannot create a world without a minecraft interface");
		}
	}

	/**
	 * Due to the limitation of the minecraft interface, you can only work with
	 * one world at a time. Creating a new world will break all previously
	 * created world objects.
	 */
	public World createWorldFromFile(File file) throws FileNotFoundException,
			IOException, IllegalStateException {
		MinecraftInterface minecraftInterface = this.minecraftInterface;
		if (minecraftInterface != null) {
			return worldBuilder.fromFile(minecraftInterface,
					SaveDirectory.from(file));
		} else {
			throw new IllegalStateException(
					"cannot create a world without a minecraft interface");
		}
	}

	public String getRecognisedVersionName() {
		MinecraftInterface minecraftInterface = this.minecraftInterface;
		if (minecraftInterface != null) {
			return minecraftInterface.getRecognisedVersion().getName();
		} else {
			return RecognisedVersion.UNKNOWN.getName();
		}
	}
}
