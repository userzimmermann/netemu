package com.adva.netemu.testemu;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.xml.bind.DatatypeConverter;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;

import com.adva.netemu.YangListBindable;
import com.adva.netemu.YangListBinding;
import com.adva.netemu.YangListBound;


@YangListBound(
        context = NetEmuDefined.class,
        namespace = "urn:ietf:params:xml:ns:yang:ietf-interfaces",
        value = "interfaces/interface")

public class TestInterface implements YangListBindable {

    static class YangBindingConnector {

        private YangBindingConnector() {}
    }

    @Nonnull
    private static final Class<? extends InterfaceType> IETF_INTERFACE_TYPE = EthernetCsmacd.class;

    @Nonnull
    private final TestInterface$YangListBinding yangBinding;

    @Nonnull @Override
    public Optional<YangListBinding<?, ?, ?>> getYangListBinding() {
        return Optional.of(this.yangBinding);
    }

    @Nonnull
    private final NetworkInterface adapter;

    @Nonnull
    public String getName() {
        return this.adapter.getName();
    }

    @Nonnull
    private final AtomicBoolean _enabled = new AtomicBoolean(false);

    public boolean isEnabled() {
        return this._enabled.get();
    }

    public boolean isDisabled() {
        return !this._enabled.get();
    }

    private TestInterface(@Nonnull final NetworkInterface adapter) {
        this.adapter = adapter;

        @Nonnull final Optional<String> macAddress;
        try {
            macAddress = Optional.ofNullable(adapter.getHardwareAddress()).map(DatatypeConverter::printHexBinary)
                    .map(hex -> hex.replaceAll("(..)", ":$1").substring(1));

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        @Nonnull final var connector = new YangBindingConnector();
        this.yangBinding = TestInterface$YangListBinding.withKey(TestInterface$Yang.ListKey.from(adapter.getName()))

                .appliesConfigurationDataUsing(connector, data -> {
                    data.getEnabled().ifPresent(this._enabled::set);
                })

                .appliesOperationalDataUsing(connector, data -> {
                    data.getEnabled().ifPresent(this._enabled::set);
                })

                .providesOperationalDataUsing(connector, builder -> builder
                        .setType(IETF_INTERFACE_TYPE)
                        .setName(adapter.getName())
                        .setEnabled(this._enabled.get())

                        .setDescription(adapter.getDisplayName())
                        .setPhysAddress(macAddress.map(PhysAddress::new).orElse(null)));
    }

    @Nonnull
    public static TestInterface from(@Nonnull final NetworkInterface adapter) {
        return new TestInterface(adapter);
    }

    @Nonnull
    public static TestInterface withName(@Nonnull final String name) {
        try {
            return new TestInterface(NetworkInterface.getByName(name));

        } catch (SocketException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Nonnull
    public static TestInterface fromConfigurationData(@Nonnull final TestInterface$Yang.Data data) {
        final var intf = TestInterface.withName(data.getName().orElseThrow(() -> new IllegalArgumentException(
                "No 'name' leaf value present in YANG Data")));

        intf.yangBinding.applyConfigurationData(data);
        return intf;
    }

    public void enable() {
        this._enabled.set(true);
    }

    public void disable() {
        this._enabled.set(false);
    }
}
