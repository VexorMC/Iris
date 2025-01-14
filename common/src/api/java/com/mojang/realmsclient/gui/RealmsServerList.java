/*
 * Decompiled with CFR 0.152.
 */
package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;

public class RealmsServerList
implements Iterable<RealmsServer> {
    private final Minecraft minecraft;
    private final Set<RealmsServer> removedServers = new HashSet<RealmsServer>();
    private List<RealmsServer> servers = List.of();

    public RealmsServerList(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void updateServersList(List<RealmsServer> list) {
        ArrayList<RealmsServer> arrayList = new ArrayList<RealmsServer>(list);
        arrayList.sort(new RealmsServer.McoServerComparator(this.minecraft.getUser().getName()));
        boolean bl = arrayList.removeAll(this.removedServers);
        if (!bl) {
            this.removedServers.clear();
        }
        this.servers = arrayList;
    }

    public void removeItem(RealmsServer realmsServer) {
        this.servers.remove(realmsServer);
        this.removedServers.add(realmsServer);
    }

    @Override
    public Iterator<RealmsServer> iterator() {
        return this.servers.iterator();
    }

    public boolean isEmpty() {
        return this.servers.isEmpty();
    }
}

