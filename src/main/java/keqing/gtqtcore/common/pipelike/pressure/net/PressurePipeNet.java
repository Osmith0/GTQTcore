package keqing.gtqtcore.common.pipelike.pressure.net;


import gregtech.api.pipenet.PipeNet;
import gregtech.api.pipenet.WorldPipeNet;
import keqing.gtqtcore.api.GCYSValues;
import keqing.gtqtcore.api.capability.IPressureContainer;
import keqing.gtqtcore.common.pipelike.pressure.PressurePipeData;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public class PressurePipeNet extends PipeNet<PressurePipeData> implements IPressureContainer {

    private double netParticles = GCYSValues.EARTH_PRESSURE;
    private double volume = 1.0D;
    private double minNetPressure = Double.MAX_VALUE;
    private double maxNetPressure = Double.MIN_VALUE;

    public PressurePipeNet(WorldPipeNet<PressurePipeData, ? extends PipeNet> world) {
        super(world);
    }

    @Override
    protected void writeNodeData(@Nonnull PressurePipeData pressurePipeData, @Nonnull NBTTagCompound nbt) {
        nbt.setDouble("MinP", pressurePipeData.getMinPressure());
        nbt.setDouble("MaxP", pressurePipeData.getMaxPressure());
        nbt.setDouble("Volume", pressurePipeData.getVolume());
    }

    @Override
    protected PressurePipeData readNodeData(@Nonnull NBTTagCompound nbt) {
        return new PressurePipeData(nbt.getDouble("MinP"), nbt.getDouble("MaxP"), nbt.getDouble("Volume"));
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound compound = super.serializeNBT();
        compound.setDouble("minNetP", minNetPressure);
        compound.setDouble("maxNetP", maxNetPressure);
        compound.setDouble("Volume", volume);
        compound.setDouble("Particles", netParticles);
        return compound;
    }


    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        super.deserializeNBT(nbt);
        this.minNetPressure = nbt.getDouble("minNetP");
        this.maxNetPressure = nbt.getDouble("maxNetP");
        this.volume = nbt.getDouble("Volume");
        this.netParticles = nbt.getDouble("Particles");
    }

    @Override
    protected void onNodeConnectionsUpdate() {
        super.onNodeConnectionsUpdate();
        this.minNetPressure = getAllNodes().values().stream().mapToDouble(node -> node.data.getMinPressure()).max().orElse(Double.MAX_VALUE);
        this.maxNetPressure = getAllNodes().values().stream().mapToDouble(node -> node.data.getMaxPressure()).min().orElse(Double.MIN_VALUE);
        final double oldVolume = getVolume();
        this.volume = Math.max(1, getAllNodes().values().stream().mapToDouble(node -> node.data.getVolume()).sum());
        this.netParticles *= getVolume() / oldVolume;
    }

    @Override
    public void onPipeConnectionsUpdate() {
        super.onPipeConnectionsUpdate();
    }

    @Override
    public double getParticles() {
        return netParticles;
    }

    @Override
    public void setParticles(double amount) {
        this.netParticles = amount;
    }

    @Override
    public double getVolume() {
        return volume;
    }

    @Override
    public boolean changeParticles(double amount, boolean simulate) {
        if (simulate) return isPressureSafe(getPressureForParticles(getParticles() + amount));
        setParticles(getParticles() + amount);
        PressureNetWalker.checkPressure(getWorldData(), getAllNodes().keySet().iterator().next(), getPressure());
        return isPressureSafe();
    }

    public void onLeak() {
        if (getPressure() < GCYSValues.EARTH_PRESSURE) changeParticles(getLeakRate(), false);
        else if (getPressure() > GCYSValues.EARTH_PRESSURE) changeParticles(-getLeakRate(), false);
    }

    public double getLeakRate() {
        return 1000.0D;
    }

    @Override
    public double getMinPressure() {
        return minNetPressure;
    }

    @Override
    public double getMaxPressure() {
        return maxNetPressure;
    }
}