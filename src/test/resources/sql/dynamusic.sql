
CREATE TABLE dynamusic_artist (
        id                      VARCHAR(32)     not null,
        name                    VARCHAR(100)    null,
        description             LONG VARCHAR    null,
        image                   VARCHAR(32)     null,
        primary key(id)
        );

CREATE TABLE dynamusic_song (
        id                      VARCHAR(32)     not null,
        title                   VARCHAR(100)    null,
        genre                   INTEGER         null,
        download                VARCHAR(100)    null,
--        artist                  VARCHAR(32)     null references dynamusic_artist(id),
        artist                  VARCHAR(32)     null,
        lyrics                  LONG VARCHAR    null,
        primary key(id)
);

CREATE TABLE dynamusic_album (
        id                      VARCHAR(32)     not null,
        title                   VARCHAR(100)    null,
        cover                   VARCHAR(100)    null,
        artist                  VARCHAR(32)     null references dynamusic_artist(id),
--        artist                  VARCHAR(32)     null,
        published		TIMESTAMP	null,
        description             LONG VARCHAR    null,
        primary key(id)
);

CREATE TABLE dynamusic_album_songs (
        album_id                VARCHAR(32)     not null references dynamusic_album(id),
        song_list               VARCHAR(32)     not null references dynamusic_song(id),
        primary key(album_id, song_list)
);

CREATE INDEX dynamusic_album_songs_album_idx ON dynamusic_album_songs(album_id);


CREATE TABLE dynamusic_venue (
        id                      VARCHAR(32)     not null,
        name                    VARCHAR(100)    null,
        description             LONG VARCHAR    null,
        street1                 VARCHAR(100)    null,
        street2                 VARCHAR(100)    null,
        state                   VARCHAR(32)     null,
        city                    VARCHAR(50)     null,
        zip                     VARCHAR(10)     null,
        phone			VARCHAR(20)	null,
        image                   VARCHAR(100)    null,
        url                     VARCHAR(100)    null,
        primary key(id)
);

CREATE TABLE dynamusic_venue_eventtypes (
        venue_id                VARCHAR(32)     not null,
        event_type              VARCHAR(32)     not null,
        primary key(venue_id, event_type)
);

CREATE TABLE dynamusic_concert (
        id                      VARCHAR(32)     not null,
        name                    VARCHAR(100)    null,
        description             LONG VARCHAR    null,
        venue                   VARCHAR(32)     null references dynamusic_venue(id),
        image                   VARCHAR(100)    null,
        event_date              TIMESTAMP       null,
        primary key(id)
);

CREATE TABLE dynamusic_concert_artists (
        concert_id              VARCHAR(32)     not null references dynamusic_concert(id),
--        artist_id               VARCHAR(32)     not null references dynamusic_artist(id),
        artist_id               VARCHAR(32)     not null,
        primary key(concert_id, artist_id)
);

CREATE INDEX dynamusic_concert_artists_concert_idx ON dynamusic_concert_artists(concert_id);


CREATE TABLE dynamusic_user (
        user_id                 VARCHAR(32)     not null references dps_user(id),
        info                    LONG VARCHAR    null,
        share_profile           NUMERIC(1)      null,
        subscriber              NUMERIC(1)      null,
        CHECK (share_profile in (0, 1)),
        primary key(user_id)
);

CREATE INDEX dynamusic_user_user_idx ON dynamusic_user(user_id);

CREATE TABLE dynamusic_prefgenres (
        id                      VARCHAR(32)     not null references dps_user(id),
        genre                   VARCHAR(32)     not null,
        primary key(id, genre)
);

CREATE INDEX dynamusic_prefgenres_idx ON dynamusic_prefgenres(id);

CREATE TABLE dynamusic_viewedartists (
        user_id                 VARCHAR(32)     not null references dps_user(id),
--        artist_id               VARCHAR(32)     not null references dynamusic_artist(id),
        artist_id               VARCHAR(32)     not null,
        primary key(user_id, artist_id)
);

CREATE INDEX dynamusic_viewedartists_idx ON dynamusic_viewedartists(user_id);

CREATE TABLE dynamusic_viewedsongs (
        user_id                 VARCHAR(32)     not null references dps_user(id),
--        song_id               VARCHAR(32)     not null references dynamusic_song(id),
        song_id               VARCHAR(32)     not null,
        primary key(user_id, song_id)
);

CREATE INDEX dynamusic_viewedsongs_idx ON dynamusic_viewedsongs(user_id);

CREATE TABLE dynamusic_playlist (
        id                      VARCHAR(32)     not null,
        name                    VARCHAR(100)    null,
        publish                 NUMERIC(1)      null,
        description             LONG VARCHAR    null,
           CHECK (publish in (0, 1)),
        primary key(id)
);

CREATE TABLE dynamusic_playlist_song (
        pl_id                   VARCHAR(32)     not null references dynamusic_playlist(id),
        song_id                 VARCHAR(32)     not null references dynamusic_song(id),
        primary key(song_id, pl_id)
);

CREATE INDEX dynamusic_playlist_song_pl_idx ON dynamusic_playlist_song(pl_id);

CREATE TABLE dynamusic_user_playlists (
        user_id                 VARCHAR(32)     not null references dps_user(id),
        pl_id                   VARCHAR(32)     not null references dynamusic_playlist(id),
        primary key(user_id, pl_id)
);

CREATE INDEX dynamusic_user_playlists_user_idx ON dynamusic_user_playlists(user_id);

commit work;
