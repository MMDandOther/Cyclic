package com.lothrazar.cyclic.block.generatorsolar;

import com.lothrazar.cyclic.block.TileBlockEntityCyclic;
import com.lothrazar.cyclic.capabilities.CustomEnergyStorage;
import com.lothrazar.cyclic.capabilities.ItemStackHandlerWrapper;
import com.lothrazar.cyclic.registry.TileRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileGeneratorSolar extends TileBlockEntityCyclic {

  static enum Fields {
    REDSTONE, FLOWING;
  }

  static final int MAX = 64000;
  CustomEnergyStorage energy = new CustomEnergyStorage(MAX, MAX);
  private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
  ItemStackHandler inputSlots = new ItemStackHandler(1);
  ItemStackHandler outputSlots = new ItemStackHandler(0);
  private ItemStackHandlerWrapper inventory = new ItemStackHandlerWrapper(inputSlots, outputSlots);
  private LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

  public TileGeneratorSolar(BlockPos pos, BlockState state) {
    super(TileRegistry.GENERATOR_SOLAR.get(), pos, state);
  }

  public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, TileGeneratorSolar e) {
    e.tick();
  }

  public static <E extends BlockEntity> void clientTick(Level level, BlockPos blockPos, BlockState blockState, TileGeneratorSolar e) {
    e.tick();
  }

  //  @Override
  public void tick() {
    this.syncEnergy();
    if (level.isClientSide) {
      return;
    }
    moveEnergy(Direction.DOWN, 100);
    timer--;
    if (timer == 0 || BlockGeneratorSolar.TIMEOUT.get() == 0) {
      tryConsumeFuel();
    }
    if (timer <= 0) {
      timer = BlockGeneratorSolar.TIMEOUT.get();
    }
  }

  private void tryConsumeFuel() {
    if (this.level.isDay() && this.level.canSeeSkyFromBelowWater(this.getBlockPos())) {
      setLitProperty(true);
      int receive = BlockGeneratorSolar.ENERGY_GENERATE.get();
      if (this.level.isThundering()) {
        receive = receive / 4;
      }
      else if (this.level.isRaining()) {
        receive = receive / 2;
      }
      if (receive < 1) {
        receive = 1;
      }
      if (energy.receiveEnergy(receive, true) == receive) {
        //we have room in the tank, burn one tck and fill up 
        energy.receiveEnergy(receive, false);
      }
    }
    else {
      setLitProperty(false);
    }
  }

  @Override
  public void invalidateCaps() {
    energyCap.invalidate();
    inventoryCap.invalidate();
    super.invalidateCaps();
  }

  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
    if (cap == CapabilityEnergy.ENERGY && side == Direction.DOWN) {
      return energyCap.cast();
    }
    if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return inventoryCap.cast();
    }
    return super.getCapability(cap, side);
  }

  @Override
  public void load(CompoundTag tag) {
    energy.deserializeNBT(tag.getCompound(NBTENERGY));
    inventory.deserializeNBT(tag.getCompound(NBTINV));
    super.load(tag);
  }

  @Override
  public void saveAdditional(CompoundTag tag) {
    tag.put(NBTENERGY, energy.serializeNBT());
    tag.put(NBTINV, inventory.serializeNBT());
    super.saveAdditional(tag);
  }

  @Override
  public int getField(int id) {
    switch (Fields.values()[id]) {
      case REDSTONE:
        return this.needsRedstone;
      case FLOWING:
        return this.flowing;
    }
    return 0;
  }

  @Override
  public void setField(int field, int value) {
    switch (Fields.values()[field]) {
      case REDSTONE:
        this.needsRedstone = value % 2;
      break;
      case FLOWING:
        this.flowing = value;
      break;
    }
  }

  public int getEnergyMax() {
    return TileGeneratorSolar.MAX;
  }
}
