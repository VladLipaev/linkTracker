package backend.academy.linktracker.bot.handler.dialog;

public enum UserState {
    BASE,
    WAITING_FOR_TRACK_LINK,
    WAITING_FOR_TRACK_TAGS,
    WAITING_FOR_LIST_TAG,
    WAITING_FOR_UNTRACK_LINK
}
