package backend.academy.linktracker.bot.handler.dialog;

public record UserSession(UserState state, String tempLink) {

    public static UserSession base() {
        return new UserSession(UserState.BASE, null);
    }
}
