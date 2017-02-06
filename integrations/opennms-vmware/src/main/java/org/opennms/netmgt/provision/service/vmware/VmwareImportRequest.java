/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.provision.service.vmware;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.opennms.netmgt.provision.persist.requisition.Requisition;

/**
 * Options that control how the requisition is generated.
 *
 * @author Christian Pape <Christian.Pape@informatik.hs-fulda.de>
 * @author Alejandro Galue <agalue@opennms.org>
 */
public class VmwareImportRequest {

    private static final String VMWARE_HOSTSYSTEM_SERVICES = "hostSystemServices";
    private static final String VMWARE_VIRTUALMACHINE_SERVICES = "virtualMachineServices";

    private String hostname = null;
    private String username = null;
    private String password = null;
    private String foreignSource = null;

    private boolean importVMPoweredOn = true;
    private boolean importVMPoweredOff = false;
    private boolean importVMSuspended = false;

    private boolean importHostPoweredOn = true;
    private boolean importHostPoweredOff = false;
    private boolean importHostStandBy = false;
    private boolean importHostUnknown = false;

    private boolean persistIPv4 = true;
    private boolean persistIPv6 = true;

    private boolean persistVMs = true;
    private boolean persistHosts = true;

    private boolean topologyPortGroups = false;
    private boolean topologyNetworks = true;
    private boolean topologyDatastores = true;

    private String[] hostSystemServices = new String[]{"VMware-ManagedEntity", "VMware-HostSystem", "VMwareCim-HostSystem"};
    private String[] virtualMachineServices = new String[]{"VMware-ManagedEntity", "VMware-VirtualMachine"};

    private Map<String, String> customAttributes;
    private String oldKey;
    private String oldValue;

    private Requisition existingRequisition;

    public VmwareImportRequest() { }

    public VmwareImportRequest(Map<String, String> params) {
        setHostname(params.get("host"));
        setUsername(params.get("username"));
        setPassword(params.get("password"));

        boolean importVMOnly = queryParameter(params, "importVMOnly", false);
        boolean importHostOnly = queryParameter(params, "importHostOnly", false);

        if (importHostOnly && importVMOnly) {
            throw new IllegalArgumentException("importHostOnly and importVMOnly can't be true simultaneously");
        }
        if (importHostOnly) {
            setPersistVMs(false);
        }
        if (importVMOnly) {
            setPersistHosts(false);
        }

        boolean importIPv4Only = queryParameter(params, "importIPv4Only", false);
        boolean importIPv6Only = queryParameter(params, "importIPv6Only", false);

        if (importIPv4Only && importIPv6Only) {
            throw new IllegalArgumentException("importIPv4Only and importIPv6Only can't be true simultaneously");
        }
        if (importIPv4Only) {
            setPersistIPv6(false);
        }
        if (importIPv6Only) {
            setPersistIPv4(false);
        }

        setTopologyPortGroups(queryParameter(params, "topologyPortGroups", false));
        setTopologyNetworks(queryParameter(params, "topologyNetworks", true));
        setTopologyDatastores(queryParameter(params, "topologyDatastores", true));

        setImportVMPoweredOn(queryParameter(params, "importVMPoweredOn", true));
        setImportVMPoweredOff(queryParameter(params, "importVMPoweredOff", false));
        setImportVMSuspended(queryParameter(params, "importVMSuspended", false));

        setImportHostPoweredOn(queryParameter(params, "importHostPoweredOn", true));
        setImportHostPoweredOff(queryParameter(params, "importHostPoweredOff", false));
        setImportHostStandBy(queryParameter(params, "importHostStandBy", false));
        setImportHostUnknown(queryParameter(params, "importHostUnknown", false));

        if (queryParameter(params, "importHostAll", false)) {
            setImportHostPoweredOn(true);
            setImportHostPoweredOff(true);
            setImportHostStandBy(true);
            setImportHostUnknown(true);
        }

        if (queryParameter(params, "importVMAll", false)) {
            setImportHostPoweredOff(true);
            setImportHostPoweredOn(true);
            setImportVMSuspended(true);
        }

        String path = params.get("path");

        path = path.replaceAll("^/", "");
        path = path.replaceAll("/$", "");

        String[] pathElements = path.split("/");

        if (pathElements.length == 1) {
            if ("".equals(pathElements[0])) {
                setForeignSource("vmware-" + getHostname());
            } else {
                setForeignSource(pathElements[0]);
            }
        } else {
            throw new IllegalArgumentException("Error processing path element of URL (vmware://host[/foreign-source]?keyA=valueA;keyB=valueB;...)");
        }

        // get services to be added to host systems
        if (params.get(VMWARE_HOSTSYSTEM_SERVICES) != null) {
            setHostSystemServices(params.get(VMWARE_HOSTSYSTEM_SERVICES).split(","));
        }

        // get services to be added to virtual machines
        if (params.get(VMWARE_VIRTUALMACHINE_SERVICES) != null) {
            setVirtualMachineServices(params.get(VMWARE_VIRTUALMACHINE_SERVICES).split(","));
        }

        Map<String, String> customAttributes = new HashMap<>();
        for (String k : params.keySet()) {
            if (k.startsWith("_")) {
                customAttributes.put(k, params.get(k));
            }
        }
        setCustomAttributes(customAttributes);
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    public String getHostname() {
        return hostname;
    }
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getForeignSource() {
        return foreignSource;
    }
    public void setForeignSource(String foreignSource) {
        this.foreignSource = foreignSource;
    }
    public boolean isImportVMPoweredOn() {
        return importVMPoweredOn;
    }
    public void setImportVMPoweredOn(boolean importVMPoweredOn) {
        this.importVMPoweredOn = importVMPoweredOn;
    }
    public boolean isImportVMPoweredOff() {
        return importVMPoweredOff;
    }
    public void setImportVMPoweredOff(boolean importVMPoweredOff) {
        this.importVMPoweredOff = importVMPoweredOff;
    }
    public boolean isImportVMSuspended() {
        return importVMSuspended;
    }
    public void setImportVMSuspended(boolean importVMSuspended) {
        this.importVMSuspended = importVMSuspended;
    }
    public boolean isImportHostPoweredOn() {
        return importHostPoweredOn;
    }
    public void setImportHostPoweredOn(boolean importHostPoweredOn) {
        this.importHostPoweredOn = importHostPoweredOn;
    }
    public boolean isImportHostPoweredOff() {
        return importHostPoweredOff;
    }
    public void setImportHostPoweredOff(boolean importHostPoweredOff) {
        this.importHostPoweredOff = importHostPoweredOff;
    }
    public boolean isImportHostStandBy() {
        return importHostStandBy;
    }
    public void setImportHostStandBy(boolean importHostStandBy) {
        this.importHostStandBy = importHostStandBy;
    }
    public boolean isImportHostUnknown() {
        return importHostUnknown;
    }
    public void setImportHostUnknown(boolean importHostUnknown) {
        this.importHostUnknown = importHostUnknown;
    }
    public boolean isPersistIPv4() {
        return persistIPv4;
    }
    public void setPersistIPv4(boolean persistIPv4) {
        this.persistIPv4 = persistIPv4;
    }
    public boolean isPersistIPv6() {
        return persistIPv6;
    }
    public void setPersistIPv6(boolean persistIPv6) {
        this.persistIPv6 = persistIPv6;
    }
    public boolean isPersistVMs() {
        return persistVMs;
    }
    public void setPersistVMs(boolean persistVMs) {
        this.persistVMs = persistVMs;
    }
    public boolean isPersistHosts() {
        return persistHosts;
    }
    public void setPersistHosts(boolean persistHosts) {
        this.persistHosts = persistHosts;
    }
    public boolean isTopologyPortGroups() {
        return topologyPortGroups;
    }
    public void setTopologyPortGroups(boolean topologyPortGroups) {
        this.topologyPortGroups = topologyPortGroups;
    }
    public boolean isTopologyNetworks() {
        return topologyNetworks;
    }
    public void setTopologyNetworks(boolean topologyNetworks) {
        this.topologyNetworks = topologyNetworks;
    }
    public boolean isTopologyDatastores() {
        return topologyDatastores;
    }
    public void setTopologyDatastores(boolean topologyDatastores) {
        this.topologyDatastores = topologyDatastores;
    }
    public String[] getHostSystemServices() {
        return hostSystemServices;
    }
    public void setHostSystemServices(String[] hostSystemServices) {
        this.hostSystemServices = hostSystemServices;
    }
    public String[] getVirtualMachineServices() {
        return virtualMachineServices;
    }
    public void setVirtualMachineServices(String[] virtualMachineServices) {
        this.virtualMachineServices = virtualMachineServices;
    }
    public Map<String, String> getCustomAttributes() {
        return customAttributes;
    }
    public void setCustomAttributes(Map<String, String> customAttributes) {
        this.customAttributes = customAttributes;
    }
    public String getOldKey() {
        return oldKey;
    }
    public void setOldKey(String oldKey) {
        this.oldKey = oldKey;
    }
    public String getOldValue() {
        return oldValue;
    }
    public Requisition getExistingRequisition() {
        return existingRequisition;
    }
    public void setExistingRequisition(Requisition existingRequisition) {
        this.existingRequisition = existingRequisition;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(hostname, username, password, foreignSource, importVMPoweredOn, importVMPoweredOff,
                importVMSuspended, importHostPoweredOn, importHostPoweredOff, importHostStandBy, importHostUnknown,
                persistIPv4, persistIPv6, persistVMs, persistHosts, topologyPortGroups, topologyNetworks,
                topologyDatastores, hostSystemServices, virtualMachineServices, customAttributes, oldKey, oldValue,
                existingRequisition);
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof VmwareImportRequest)) {
            return false;
        }
        VmwareImportRequest castOther = (VmwareImportRequest) other;
        return Objects.equals(hostname, castOther.hostname)
                && Objects.equals(username, castOther.username)
                && Objects.equals(password, castOther.password)
                && Objects.equals(foreignSource, castOther.foreignSource)
                && Objects.equals(importVMPoweredOn, castOther.importVMPoweredOn)
                && Objects.equals(importVMPoweredOff, castOther.importVMPoweredOff)
                && Objects.equals(importVMSuspended, castOther.importVMSuspended)
                && Objects.equals(importHostPoweredOn, castOther.importHostPoweredOn)
                && Objects.equals(importHostPoweredOff, castOther.importHostPoweredOff)
                && Objects.equals(importHostStandBy, castOther.importHostStandBy)
                && Objects.equals(importHostUnknown, castOther.importHostUnknown)
                && Objects.equals(persistIPv4, castOther.persistIPv4)
                && Objects.equals(persistIPv6, castOther.persistIPv6)
                && Objects.equals(persistVMs, castOther.persistVMs)
                && Objects.equals(persistHosts, castOther.persistHosts)
                && Objects.equals(topologyPortGroups, castOther.topologyPortGroups)
                && Objects.equals(topologyNetworks, castOther.topologyNetworks)
                && Objects.equals(topologyDatastores, castOther.topologyDatastores)
                && Arrays.equals(hostSystemServices, castOther.hostSystemServices)
                && Arrays.equals(virtualMachineServices, castOther.virtualMachineServices)
                && Objects.equals(customAttributes, castOther.customAttributes)
                && Objects.equals(oldKey, castOther.oldKey) && Objects.equals(oldValue, castOther.oldValue)
                && Objects.equals(existingRequisition, castOther.existingRequisition);
    }

    private static boolean queryParameter(Map<String, String> parms, String key, boolean defaultValue) {
        if (parms.get(key) == null) {
            return defaultValue;
        } else {
            String value = parms.get(key).toLowerCase();

            return ("yes".equals(value) || "true".equals(value) || "on".equals(value) || "1".equals(value));
        }
    }
}
