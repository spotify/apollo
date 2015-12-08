package com.spotify.apollo.example.data;

public class Album {
  private final String name;
  private final Artist artist;

  public String getName() {
    return name;
  }

  public Artist getArtist() {
    return artist;
  }

  public Album(String name, Artist artist) {
    this.name = name;
    this.artist = artist;
  }
}
