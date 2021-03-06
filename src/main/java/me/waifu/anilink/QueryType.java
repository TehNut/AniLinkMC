package me.waifu.anilink;

import com.google.gson.JsonObject;
import me.waifu.graphquery.GraphQLQuery;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.net.MalformedURLException;
import java.util.function.Consumer;
import java.util.function.Function;

public enum QueryType {
    ANIME($ -> prepMediaQuery($, "ANIME"), QueryType::createMediaComponent),
    MANGA($ -> prepMediaQuery($, "MANGA"), QueryType::createMediaComponent),
    CHARACTER($ -> {
        prepQuery($);

        $.withObject(root -> root.withObject("Character", character -> {
            character.withArgument("id", "$id");
            character.withArgument("search", "$name");

            character.withField("description");
            character.withField("siteUrl").withAlias("url");
            character.withObject("name", name -> {
                name.withField("first");
                name.withField("last");
            });
        }));
    }, json -> {
        if (json.isNull("Character"))
            return null;

        JsonObjectWrapper charData = json.getObject("Character");

        String name = charData.getObject("name").getString("first");
        if (!charData.getObject("name").isNull("last"))
            name += " " + charData.getObject("name").getString("last");

        ITextComponent hoverData = new TextComponentString("");
        hoverData.appendSibling(new TextComponentString(name + "\n").setStyle(new Style().setUnderlined(true)));

        String description = charData.getString("description");
        description = description.replace("<br>", "");
        if (description.contains("~!"))
            description = description.substring(0, description.indexOf("~!"));
        if (description.length() > 450)
            description = description.substring(0, 450) + "...";
        hoverData.appendSibling(new TextComponentString(description));

        return new TextComponentString("[" + name + "]").setStyle(new Style()
                .setColor(TextFormatting.DARK_AQUA)
                .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, charData.getString("url")))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverData))
        );
    }),
    ;

    private final Consumer<GraphQLQuery> queryBuilder;
    private final Function<JsonObjectWrapper, ITextComponent> componentProvider;
    private GraphQLQuery query;

    QueryType(Consumer<GraphQLQuery> queryBuilder, Function<JsonObjectWrapper, ITextComponent> componentProvider) {
        this.queryBuilder = queryBuilder;
        this.componentProvider = componentProvider;
    }

    public GraphQLQuery getQuery() {
        return query == null ? query = new GraphQLQuery(GraphQLQuery.RequestType.QUERY, queryBuilder) : query;
    }

    public ITextComponent getTextComponent(JsonObject jsonObject) {
        return componentProvider.apply(new JsonObjectWrapper(jsonObject));
    }

    private static void prepQuery(GraphQLQuery query) {
        query.withArgument("id", "Int");
        query.withArgument("name", "String");

        try {
            query.withUrl("https://graphql.anilist.co");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static void prepMediaQuery(GraphQLQuery query, String listType) {
        prepQuery(query);

        query.withObject(root -> root.withObject("Media", media -> {
            media.withArgument("id", "$id");
            media.withArgument("search", "$name");
            media.withArgument("type", listType);

            media.withObject("title", title -> title.withField("romaji"));
            media.withField("description");
            media.withField("isAdult");
            media.withField("siteUrl").withAlias("url");
        }));
    }

    private static ITextComponent createMediaComponent(JsonObjectWrapper json) {
        if (json.isNull("Media"))
            return null;

        JsonObjectWrapper mediaData = json.getObject("Media");
        if (AniLink.Settings.hardBlockNsfw && mediaData.getBoolean("isAdult"))
            return null;

        ITextComponent hoverData = new TextComponentString("");
        hoverData.appendSibling(new TextComponentString(mediaData.getObject("title").getString("romaji") + "\n").setStyle(new Style().setUnderlined(true)));

        if (AniLink.Settings.blockNsfw && mediaData.getBoolean("isAdult"))
            hoverData.appendSibling(new TextComponentTranslation("chat.anilink:nsfw_link").setStyle(new Style().setColor(TextFormatting.RED)));
        else {
            String description = mediaData.getString("description");
            description = description.replace("<br>", "");
            if (description.contains("~!"))
                description = description.substring(0, description.indexOf("~!"));
            if (description.length() > 450)
                description = description.substring(0, 450) + "...";
            hoverData.appendSibling(new TextComponentString(description));
        }

        return new TextComponentString("[" + mediaData.getObject("title").getString("romaji") + "]").setStyle(new Style()
                .setColor(TextFormatting.DARK_AQUA)
                .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, mediaData.getString("url")))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverData))
        );
    }
}
