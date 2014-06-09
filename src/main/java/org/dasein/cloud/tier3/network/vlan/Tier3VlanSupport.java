package org.dasein.cloud.tier3.network.vlan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.OperationNotSupportedException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.ResourceStatus;
import org.dasein.cloud.Tag;
import org.dasein.cloud.identity.ServiceAction;
import org.dasein.cloud.network.IPVersion;
import org.dasein.cloud.network.NICCreateOptions;
import org.dasein.cloud.network.NetworkInterface;
import org.dasein.cloud.network.Networkable;
import org.dasein.cloud.network.RawAddress;
import org.dasein.cloud.network.RoutingTable;
import org.dasein.cloud.network.Subnet;
import org.dasein.cloud.network.SubnetCreateOptions;
import org.dasein.cloud.network.VLAN;
import org.dasein.cloud.network.VLANState;
import org.dasein.cloud.network.VLANSupport;
import org.dasein.cloud.tier3.APIHandler;
import org.dasein.cloud.tier3.APIResponse;
import org.dasein.cloud.tier3.Tier3;
import org.dasein.cloud.util.APITrace;
import org.json.JSONException;
import org.json.JSONObject;

public class Tier3VlanSupport implements VLANSupport {
	static private final Logger logger = Tier3.getLogger(Tier3VlanSupport.class);
	private Tier3 provider;

	public Tier3VlanSupport(Tier3 provider) {
		this.provider = provider;
	}

	@Override
	public String[] mapServiceAction(ServiceAction action) {
		return new String[0];
	}

	@Override
	public void addRouteToAddress(String toRoutingTableId, IPVersion version, String destinationCidr, String address)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void addRouteToGateway(String toRoutingTableId, IPVersion version, String destinationCidr, String gatewayId)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void addRouteToNetworkInterface(String toRoutingTableId, IPVersion version, String destinationCidr,
			String nicId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void addRouteToVirtualMachine(String toRoutingTableId, IPVersion version, String destinationCidr, String vmId)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public boolean allowsNewNetworkInterfaceCreation() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean allowsNewVlanCreation() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean allowsNewSubnetCreation() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean allowsMultipleTrafficTypesOverSubnet() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean allowsMultipleTrafficTypesOverVlan() throws CloudException, InternalException {
		return false;
	}

	@Override
	public void assignRoutingTableToSubnet(String subnetId, String routingTableId) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void assignRoutingTableToVlan(String vlanId, String routingTableId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void attachNetworkInterface(String nicId, String vmId, int index) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String createInternetGateway(String forVlanId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String createRoutingTable(String forVlanId, String name, String description) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NetworkInterface createNetworkInterface(NICCreateOptions options) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Subnet createSubnet(String cidr, String inProviderVlanId, String name, String description)
			throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Subnet createSubnet(SubnetCreateOptions options) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public VLAN createVlan(String cidr, String name, String description, String domainName, String[] dnsServers,
			String[] ntpServers) throws CloudException, InternalException {
		throw new OperationNotSupportedException("Vlans can only be created via the CenturyLink Cloud Control Portal.");
	}

	@Override
	public void detachNetworkInterface(String nicId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public int getMaxNetworkInterfaceCount() throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public int getMaxVlanCount() throws CloudException, InternalException {
		return -1;
	}

	@Override
	public String getProviderTermForNetworkInterface(Locale locale) {
		return "interface";
	}

	@Override
	public String getProviderTermForSubnet(Locale locale) {
		return "subnet";
	}

	@Override
	public String getProviderTermForVlan(Locale locale) {
		return "network";
	}

	@Override
	public NetworkInterface getNetworkInterface(String nicId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public RoutingTable getRoutingTableForSubnet(String subnetId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Requirement getRoutingTableSupport() throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public RoutingTable getRoutingTableForVlan(String vlanId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Subnet getSubnet(String subnetId) throws CloudException, InternalException {
		return null;
	}

	@Override
	public Requirement getSubnetSupport() throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public VLAN getVlan(String vlanId) throws CloudException, InternalException {
		if (vlanId == null) {
			return null;
		}
		APITrace.begin(provider, "getVlan");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", vlanId);
			APIResponse response = method.post("Network/GetNetworkDetails/JSON", post.toString());

			if (response == null) {
				throw new CloudException("Invalid vlan");
			}
			JSONObject json = response.getJSON();
			if (!json.getBoolean("Success")) {
				logger.warn(json.getString("Message"));
				return null;
			}
			if (json.has("NetworkDetails")) {
				return toVlan(json.getJSONObject("NetworkDetails"));
			}

			return null;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Requirement identifySubnetDCRequirement() throws CloudException, InternalException {
		return Requirement.NONE;
	}

	@Override
	public boolean isConnectedViaInternetGateway(String vlanId) throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean isNetworkInterfaceSupportEnabled() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isSubscribed() throws CloudException, InternalException {
		return true;
	}

	@Override
	public boolean isSubnetDataCenterConstrained() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean isVlanDataCenterConstrained() throws CloudException, InternalException {
		return true;
	}

	@Override
	public Collection<String> listFirewallIdsForNIC(String nicId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<ResourceStatus> listNetworkInterfaceStatus() throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfaces() throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesForVM(String forVmId) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesInSubnet(String subnetId) throws CloudException,
			InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Iterable<NetworkInterface> listNetworkInterfacesInVLAN(String vlanId) throws CloudException,
			InternalException {
		if (vlanId == null) {
			return null;
		}
		APITrace.begin(provider, "listNetworkInterfacesInVLAN");
		try {
			APIHandler method = new APIHandler(provider);
			JSONObject post = new JSONObject();
			post.put("Name", vlanId);
			APIResponse response = method.post("Network/GetNetworkDetails/JSON", post.toString());

			if (response == null) {
				throw new CloudException("Invalid vlan");
			}
			JSONObject json = response.getJSON();
			if (!json.getBoolean("Success")) {
				throw new CloudException(json.getString("Message"));
			}
			
			ArrayList<NetworkInterface> networks = new ArrayList<NetworkInterface>();

			if (json.has("NetworkDetails")) {
				networks.add(toNetwork(json.getJSONObject("NetworkDetails")));
			}

			return networks;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<Networkable> listResources(String inVlanId) throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<RoutingTable> listRoutingTables(String inVlanId) throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<Subnet> listSubnets(String inVlanId) throws CloudException, InternalException {
		return Collections.emptyList();
	}

	@Override
	public Iterable<IPVersion> listSupportedIPVersions() throws CloudException, InternalException {
		return Arrays.asList(IPVersion.IPV4);
	}

	@Override
	public Iterable<ResourceStatus> listVlanStatus() throws CloudException, InternalException {
		APITrace.begin(provider, "listVlanStatus");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Network/GetNetworks/JSON", "");

			ArrayList<ResourceStatus> vlans = new ArrayList<ResourceStatus>();

			JSONObject json = response.getJSON();
			if (json.has("Networks")) {
				for (int i = 0; i < json.getJSONArray("Networks").length(); i++) {
					ResourceStatus vlan = provider.getNetworkTranslations().toVlanStatus(
							json.getJSONArray("Networks").getJSONObject(i));
					if (vlan != null) {
						vlans.add(vlan);
					}
				}
			}

			return vlans;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public Iterable<VLAN> listVlans() throws CloudException, InternalException {
		APITrace.begin(provider, "listVlans");
		try {
			APIHandler method = new APIHandler(provider);
			APIResponse response = method.post("Network/GetNetworks/JSON", "");

			ArrayList<VLAN> vlans = new ArrayList<VLAN>();

			JSONObject json = response.getJSON();
			if (json.has("Networks")) {
				for (int i = 0; i < json.getJSONArray("Networks").length(); i++) {

					JSONObject post = new JSONObject();
					post.put("Name", json.getJSONArray("Networks").getJSONObject(i).getString("Name"));
					APIResponse detailResponse = method.post("Network/GetNetworkDetails/JSON", post.toString());

					JSONObject detailJson = detailResponse.getJSON();
					if (!detailJson.getBoolean("Success")) {
						throw new CloudException(detailJson.getString("Message"));
					}
					if (detailJson.has("NetworkDetails")) {
						vlans.add(toVlan(detailJson.getJSONObject("NetworkDetails")));
					}
				}
			}

			return vlans;
		} catch (JSONException e) {
			throw new CloudException(e);
		} finally {
			APITrace.end();
		}
	}

	@Override
	public void removeInternetGateway(String forVlanId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeNetworkInterface(String nicId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeRoute(String inRoutingTableId, String destinationCidr) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeRoutingTable(String routingTableId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeSubnet(String providerSubnetId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeVlan(String vlanId) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeVLANTags(String vlanId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void removeVLANTags(String[] vlanIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public boolean supportsInternetGatewayCreation() throws CloudException, InternalException {
		return false;
	}

	@Override
	public boolean supportsRawAddressRouting() throws CloudException, InternalException {
		return false;
	}

	@Override
	public void updateVLANTags(String vlanId, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void updateVLANTags(String[] vlanIds, Tag... tags) throws CloudException, InternalException {
		throw new OperationNotSupportedException();
	}

	private VLAN toVlan(JSONObject ob) throws CloudException, InternalException {
		if (ob == null) {
			return null;
		}
		try {
			VLAN vlan = new VLAN();

			vlan.setProviderVlanId(ob.getString("Name"));
			vlan.setProviderOwnerId(provider.getContext().getAccountNumber());
			vlan.setProviderRegionId(provider.getDataCenterServices().getDataCenter(ob.getString("Location")).getRegionId());
			vlan.setProviderDataCenterId(ob.getString("Location"));
			
			vlan.setCurrentState(VLANState.AVAILABLE);
			vlan.setName(ob.getString("Name"));
			vlan.setDescription(ob.getString("Description"));
			vlan.setCidr(ob.getString("NetworkMask"), ob.getString("Gateway"));

			vlan.setSupportedTraffic(IPVersion.IPV4);
			vlan.setDnsServers(new String[0]);
			vlan.setNtpServers(new String[0]);
			vlan.setTags(Collections.<String,String>emptyMap());

			return vlan;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}
	
	private NetworkInterface toNetwork(JSONObject ob) throws CloudException, InternalException {
		if (ob == null) {
			return null;
		}
		try {
			NetworkInterface network = new NetworkInterface();

			network.setProviderVlanId(ob.getString("Name"));
			network.setName(ob.getString("Name"));
			network.setDescription(ob.getString("Description"));
			network.setProviderDataCenterId(ob.getString("Location"));
			
			ArrayList<RawAddress> ips = new ArrayList<RawAddress>();
			if (ob.has("IPAddresses")) {
				for (int i = 0; i < ob.getJSONArray("IPAddresses").length(); i++) {
					JSONObject json = ob.getJSONArray("IPAddresses").getJSONObject(i);
					ips.add(new RawAddress(json.getString("Address")));
				}
			}
			if (ips.size() > 0) {
				network.setIpAddresses(ips.toArray(new RawAddress[ips.size()]));
			}
			
			return network;
		} catch (JSONException e) {
			throw new CloudException(e);
		}
	}
}