package org.dasein.cloud.tier3.compute.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;
import org.dasein.cloud.AsynchronousTask;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.ImageCreateOptions;
import org.dasein.cloud.compute.ImageFilterOptions;
import org.dasein.cloud.compute.MachineImage;
import org.dasein.cloud.compute.MachineImageFormat;
import org.dasein.cloud.compute.MachineImageState;
import org.dasein.cloud.compute.MachineImageSupport;
import org.dasein.cloud.compute.MachineImageType;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.util.APITrace;
import org.json.JSONException;
import org.json.JSONObject;

public class Tier3Image implements MachineImageSupport {
	static private final Logger logger = Tier3.getLogger(Tier3Image.class);
	private Tier3 provider;

	public Tier3Image(Tier3 provider) {
		this.provider = provider;
	}

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	@Override
	public void addImageShare(String providerImageId, String accountNumber) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public void addPublicShare(String providerImageId) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public String bundleVirtualMachine(String virtualMachineId, MachineImageFormat format, String bucket, String name)
			throws CloudException, InternalException {
		return null;
	}

	@Override
	public void bundleVirtualMachineAsync(String virtualMachineId, MachineImageFormat format, String bucket,
			String name, AsynchronousTask<String> trackingTask) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public MachineImage captureImage(ImageCreateOptions options) throws CloudException, InternalException {
		if (!options.getMetaData().containsKey("Password")) {
			throw new CloudException("MetaData entry for 'Password' required");
		}
		APITrace.begin(provider, "captureImage");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", options.getVirtualMachineId());
			post.put("Password", options.getMetaData().get("Password"));
			post.put("TemplateAlias", options.getName());
			APIResponse response = method.post("Server/ConvertServerToTemplate/JSON", post.toString());

			if (response == null) {
				throw new CloudException("No server was created");
			}

			post = new JSONObject();
			post.put("RequestId", response.getJSON().getInt("RequestID"));
			response = method.post("Blueprint/GetDeploymentStatus/JSON", post.toString());

			if (response == null) {
				throw new CloudException("Could not retrieve template build request");
			}

			MachineImage image = toMachineImage(response.getJSON());
			return image;
			
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void captureImageAsync(ImageCreateOptions options, AsynchronousTask<MachineImage> taskTracker)
			throws CloudException, InternalException {
		captureImage(options);
	}

	@Override
	public MachineImage getImage(String providerImageId) throws CloudException, InternalException {
		if (providerImageId == null) {
			return null;
		}
		for (MachineImage image : listImages((ImageFilterOptions) null)) {
			if (providerImageId.equals(image.getProviderMachineImageId())) {
				return image;
			}
		}
		return null;
	}

	@Override
	public MachineImage getMachineImage(String providerImageId) throws CloudException, InternalException {
		return getImage(providerImageId);
	}

	@Override
	public String getProviderTermForImage(Locale locale) {
		return "template";
	}

	@Override
	public String getProviderTermForImage(Locale locale, ImageClass cls) {
		switch (cls) {
		case KERNEL:
			return null;
		case RAMDISK:
			return null;
		default:
			break;
		}
		return getProviderTermForImage(locale);
	}

	@Override
	public String getProviderTermForCustomImage(Locale locale, ImageClass cls) {
		return getProviderTermForImage(locale, cls);
	}

	@Override
	public boolean hasPublicLibrary() {
		return false;
	}

	@Override
	public Requirement identifyLocalBundlingRequirement() throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public AsynchronousTask<String> imageVirtualMachine(String vmId, String name, String description)
			throws CloudException, InternalException {
		throw new CloudException("Utilize the captureImage method to create server templates");
	}

	@Override
	public boolean isImageSharedWithPublic(String providerImageId) throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public Iterable<ResourceStatus> listImageStatus(ImageClass cls) throws CloudException, InternalException {
		if (cls != null && cls != ImageClass.MACHINE) {
			return Collections.emptyList();
		}
		APITrace.begin(provider, "listImageStatus");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetServerTemplates/JSON", "");

			ArrayList<ResourceStatus> resources = new ArrayList<ResourceStatus>();

			JSONObject json = response.getJSON();
			if (json.has("Templates")) {
				for (int i = 0; i < json.getJSONArray("Templates").length(); i++) {
					resources.add(new ResourceStatus(json.getJSONArray("Templates").getJSONObject(i).getString("ID"),
							MachineImageState.ACTIVE));
				}
			}

			return resources;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<MachineImage> listImages(ImageFilterOptions options) throws CloudException, InternalException {
		APITrace.begin(provider, "listImages");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Server/GetServerTemplates/JSON", "");

			ArrayList<MachineImage> images = new ArrayList<MachineImage>();

			JSONObject json = response.getJSON();
			if (json.has("Templates")) {
				for (int i = 0; i < json.getJSONArray("Templates").length(); i++) {
					MachineImage image = toMachineImage(json.getJSONArray("Templates").getJSONObject(i));
					if (image != null && options.matches(image)) {
						images.add(image);
					}
				}
			}

			return images;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<MachineImage> listImages(ImageClass cls) throws CloudException, InternalException {
		if (cls != null && cls != ImageClass.MACHINE) {
			return Collections.emptyList();
		}
		return listImages((ImageFilterOptions) null);
	}

	private MachineImage toMachineImage(JSONObject ob) throws CloudException, InternalException {
		if (ob == null) {
			return null;
		}
		try {
			MachineImage image = MachineImage.getMachineImageInstance(provider.getContext().getAccountNumber(), "*", ob
					.getString("ID"), MachineImageState.ACTIVE, ob.getString("Name"), ob.getString("Description"),
					provider.getComputeTranslations().toArchitecture(ob.getString("Name")), provider
							.getComputeTranslations().toPlatform(ob.getString("Name")));

			if (ob.has("Cpu")) {
				image.setTag("Cpu", ob.get("Cpu").toString());
			}
			if (ob.has("MemoryGB")) {
				image.setTag("MemoryGB", ob.get("MemoryGB").toString());
			}
			if (ob.has("TotalDiskSpaceGB")) {
				image.setTag("TotalDiskSpaceGB", ob.get("TotalDiskSpaceGB").toString());
			}
			if (ob.has("OperatingSystem")) {
				image.setTag("OperatingSystem", ob.get("OperatingSystem").toString());
			}
			if (ob.has("RequestID")) {
				image.setTag("ResourceID", ob.get("ResourceID").toString());
			}
			

			return image;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}

	@Override
	public Iterable<MachineImage> listImages(ImageClass cls, String ownedBy) throws CloudException, InternalException {
		return listImages(cls);
	}

	@Override
	public Iterable<MachineImageFormat> listSupportedFormats() throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<MachineImageFormat> listSupportedFormatsForBundling() throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<MachineImage> listMachineImages() throws CloudException, InternalException {
		return listImages((ImageFilterOptions) null);
	}

	@Override
	public Iterable<MachineImage> listMachineImagesOwnedBy(String accountId) throws CloudException, InternalException {
		return listImages((ImageFilterOptions) null);
	}

	@Override
	public Iterable<String> listShares(String providerImageId) throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<ImageClass> listSupportedImageClasses() throws CloudException, InternalException {
        return Collections.singletonList(ImageClass.MACHINE);
	}

	@Override
	public Iterable<MachineImageType> listSupportedImageTypes() throws CloudException, InternalException {
        return Collections.singletonList(MachineImageType.VOLUME);
	}

	@Override
	public MachineImage registerImageBundle(ImageCreateOptions options) throws CloudException, InternalException {
        // unimplemented
		return null;
	}

	@Override
	public void remove(String providerImageId) throws CloudException, InternalException {
		APITrace.begin(provider, "remove");
		try {
			MachineImage image = getImage(providerImageId);
			
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", image.getName());
			method.post("Server/DeleteTemplate/JSON", post.toString());
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void remove(String providerImageId, boolean checkState) throws CloudException, InternalException {
		remove(providerImageId);
	}

	@Override
	public void removeAllImageShares(String providerImageId) throws CloudException, InternalException {
		// unimplemented

	}

	@Override
	public void removeImageShare(String providerImageId, String accountNumber) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public void removePublicShare(String providerImageId) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public Iterable<MachineImage> searchImages(String accountNumber, String keyword, Platform platform,
			Architecture architecture, ImageClass... imageClasses) throws CloudException, InternalException {
		ArrayList<MachineImage> images = new ArrayList<MachineImage>();
		for (MachineImage image : listImages((ImageFilterOptions) null)) {
			for (int i=0; i < imageClasses.length; i++) {
				// test keyword
				if (image.getName() != null) {
					if (keyword != null && image.getName().contains(keyword)) {
						if (!images.contains(image)) {
							images.add(image);
						}
					}
				}
				if (image.getDescription() != null) {
					if (keyword != null && image.getDescription().contains(keyword)) {
						if (!images.contains(image)) {
							images.add(image);
						}
					}
				}
				if (image.getTags() != null) {
					for (String key : image.getTags().keySet()) {
						if (image.getTag(key) != null && image.getTag(key).toString().contains(keyword)) {
							if (!images.contains(image)) {
								images.add(image);
							}
						}
					}
				}
				
				// test platform
				if (platform != null && image.getPlatform().compareTo(platform) == 0) {
					if (!images.contains(image)) {
						images.add(image);
					}
				}
				
				// test architecture
				if (architecture != null && image.getArchitecture().compareTo(architecture) == 0) {
					if (!images.contains(image)) {
						images.add(image);
					}
				}
				
				// test image class
				if (imageClasses[i] == image.getImageClass()) {
					if (!images.contains(image)) {
						images.add(image);
					}
				}
			}
		}
		return images;
	}

	@Override
	public Iterable<MachineImage> searchMachineImages(String keyword, Platform platform, Architecture architecture)
			throws CloudException, InternalException {
		return searchImages(null, keyword, platform, architecture, (ImageClass) null);
	}

	@Override
	public Iterable<MachineImage> searchPublicImages(ImageFilterOptions options) throws InternalException,
			CloudException {
		return null;
	}

	@Override
	public Iterable<MachineImage> searchPublicImages(String keyword, Platform platform, Architecture architecture,
			ImageClass... imageClasses) throws CloudException, InternalException {
		return null;
	}

	@Override
	public void shareMachineImage(String providerImageId, String withAccountId, boolean allow) throws CloudException,
			InternalException {
		// unimplemented
	}

	@Override
	public boolean supportsCustomImages() throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean supportsDirectImageUpload() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean supportsImageCapture(MachineImageType type) throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean supportsImageSharing() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean supportsImageSharingWithPublic() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean supportsPublicLibrary(ImageClass cls) throws CloudException, InternalException {
		return false;
	}

	@Override
	public void updateTags(String imageId, Tag... tags) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public void updateTags(String[] imageIds, Tag... tags) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public void removeTags(String imageId, Tag... tags) throws CloudException, InternalException {
		// unimplemented
	}

	@Override
	public void removeTags(String[] imageIds, Tag... tags) throws CloudException, InternalException {
		// unimplemented
	}
}