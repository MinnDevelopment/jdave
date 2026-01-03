package club.minnced.discord.jdave.interop;

import net.dv8tion.jda.api.audio.dave.DaveProtocolCallbacks;
import net.dv8tion.jda.api.audio.dave.DaveSession;
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory;
import org.jspecify.annotations.NonNull;

public class JDaveSessionFactory implements DaveSessionFactory {
    @NonNull
    @Override
    public DaveSession createDaveSession(@NonNull DaveProtocolCallbacks callbacks, long userId, long channelId) {
        return new JDaveSession(userId, channelId, callbacks);
    }
}
