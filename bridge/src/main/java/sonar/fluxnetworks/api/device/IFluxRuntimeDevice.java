package sonar.fluxnetworks.api.device;

import sonar.fluxnetworks.common.connection.TransferHandler;

/** Active network participant; does not require a world block entity. */
public interface IFluxRuntimeDevice extends IFluxDevice {
    TransferHandler getTransferHandler();
    void markEnergyChanged();
    void disconnect();
    default void sendBlockUpdate() {}
}
