package club.minnced.discord.jdave.interop;

import net.dv8tion.jda.api.audio.dave.DaveProtocolCallbacks;
import net.dv8tion.jda.api.audio.dave.DaveSession;
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory;

public class JDaveSessionFactory implements DaveSessionFactory {
    @Override
    public DaveSession createDaveSession(DaveProtocolCallbacks callbacks, long userId, long channelId) {
        return new JDaveSession(userId, channelId, callbacks);
    }
}
