package br.navegantes.navio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import br.navegantes.Navegantes;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Posições coladas (por dimensão), guardadas com o mundo. Só quem está colado entra no navio. */
public class Cola extends SavedData {
    public static final Codec<Cola> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
            Codec.LONG.listOf().optionalFieldOf("pos", List.of()).forGetter(Cola::lista)
    ).apply(instancia, Cola::new));

    public static final SavedDataType<Cola> TIPO =
            new SavedDataType<>(Navegantes.id("cola"), Cola::new, CODEC, DataFixTypes.LEVEL);

    private final LongOpenHashSet conjunto = new LongOpenHashSet();

    public Cola() {}

    private Cola(List<Long> posicoes) {
        conjunto.addAll(posicoes);
    }

    private List<Long> lista() {
        return new ArrayList<>(conjunto);
    }

    public static Cola de(ServerLevel nivel) {
        return nivel.getDataStorage().computeIfAbsent(TIPO);
    }

    public boolean contem(BlockPos pos) {
        return conjunto.contains(pos.asLong());
    }

    public void adicionar(Collection<BlockPos> posicoes) {
        for (BlockPos p : posicoes) conjunto.add(p.asLong());
        setDirty();
    }

    public void remover(Collection<BlockPos> posicoes) {
        for (BlockPos p : posicoes) conjunto.remove(p.asLong());
        setDirty();
    }
}
