package net.ridez.horror.sound;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;

// The mod's own sounds. What each one plays is set in assets/horrormod/sounds.json: for now they point at vanilla
// sound files as placeholders; a recording dropped into assets/horrormod/sounds/ and named there replaces one
// without touching the code.
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, HorrorMod.MOD_ID);

    // The Skinwalker knocking on a door (one rap; it raps three times), and what it says through the door after.
    public static final RegistryObject<SoundEvent> skinwalker_knock = register("skinwalker_knock");
    public static final RegistryObject<SoundEvent> skinwalker_door_voice = register("skinwalker_door_voice");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
