package com.kidsgame.mathapp.quiz;

import java.util.List;
import java.util.Locale;

public final class BulgarianWordCatalog {
    private static final Locale BG = Locale.forLanguageTag("bg-BG");

    private BulgarianWordCatalog() {
    }

    public static List<BulgarianWordEntry> words() {
        return WORDS;
    }

    public record BulgarianWordEntry(String word, String image, List<String> syllables, int level) {
        public BulgarianWordEntry(String word, String image, List<String> syllables) {
            this(word, image, syllables, defaultLevel(word));
        }

        public BulgarianWordEntry {
            if (level < 1 || level > 10) {
                throw new IllegalArgumentException("Bulgarian word level must be between 1 and 10.");
            }
        }

        public int letterCount() {
            return normalizedWord().length();
        }

        public String normalizedWord() {
            return normalize(word);
        }

        public List<String> letters() {
            return normalizedWord().codePoints()
                    .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                    .toList();
        }

        public boolean hasEnoughSyllablesForSyllableGame() {
            return syllables.size() >= 2;
        }
    }

    private static int defaultLevel(String word) {
        int length = normalize(word).length();
        return Math.max(1, Math.min(10, length - 2));
    }

    private static String normalize(String word) {
        return word.toUpperCase(BG).replace(" ", "").replace("-", "");
    }

    private static final List<BulgarianWordEntry> WORDS = List.of(
            new BulgarianWordEntry("око", "👁️", List.of("О", "КО")),
            new BulgarianWordEntry("ухо", "👂", List.of("У", "ХО")),
            new BulgarianWordEntry("дом", "🏠", List.of("ДОМ")),
            new BulgarianWordEntry("нос", "👃", List.of("НОС")),
            new BulgarianWordEntry("сок", "🧃", List.of("СОК")),
            new BulgarianWordEntry("мед", "🍯", List.of("МЕД")),
            new BulgarianWordEntry("лъв", "🦁", List.of("ЛЪВ")),
            new BulgarianWordEntry("рак", "🦀", List.of("РАК")),
            new BulgarianWordEntry("дърво", "🌳", List.of("ДЪР", "ВО")),
            new BulgarianWordEntry("лук", "🧅", List.of("ЛУК")),
            new BulgarianWordEntry("чай", "🍵", List.of("ЧАЙ")),
            new BulgarianWordEntry("сол", "🧂", List.of("СОЛ")),
            new BulgarianWordEntry("зъб", "🦷", List.of("ЗЪБ")),
            new BulgarianWordEntry("лед", "🧊", List.of("ЛЕД")),
            new BulgarianWordEntry("куче", "🐶", List.of("КУ", "ЧЕ")),
            new BulgarianWordEntry("риба", "🐟", List.of("РИ", "БА")),
            new BulgarianWordEntry("жаба", "🐸", List.of("ЖА", "БА")),
            new BulgarianWordEntry("стол", "🪑", List.of("СТОЛ")),
            new BulgarianWordEntry("вода", "🚰", List.of("ВО", "ДА")),
            new BulgarianWordEntry("лиса", "🦊", List.of("ЛИ", "СА")),
            new BulgarianWordEntry("крак", "🦶", List.of("КРАК")),
            new BulgarianWordEntry("хляб", "🍞", List.of("ХЛЯБ")),
            new BulgarianWordEntry("коза", "🐐", List.of("КО", "ЗА")),
            new BulgarianWordEntry("гъба", "🍄", List.of("ГЪ", "БА")),
            new BulgarianWordEntry("пате", "🐤", List.of("ПА", "ТЕ")),
            new BulgarianWordEntry("ръка", "✋", List.of("РЪ", "КА")),
            new BulgarianWordEntry("яйце", "🥚", List.of("ЯЙ", "ЦЕ")),
            new BulgarianWordEntry("влак", "🚂", List.of("ВЛАК")),
            new BulgarianWordEntry("кола", "🚗", List.of("КО", "ЛА")),
            new BulgarianWordEntry("ключ", "🔑", List.of("КЛЮЧ")),
            new BulgarianWordEntry("луна", "🌙", List.of("ЛУ", "НА")),
            new BulgarianWordEntry("топка", "⚽", List.of("ТОП", "КА")),
            new BulgarianWordEntry("котка", "🐱", List.of("КОТ", "КА")),
            new BulgarianWordEntry("кукла", "🧸", List.of("КУК", "ЛА")),
            new BulgarianWordEntry("книга", "📘", List.of("КНИ", "ГА")),
            new BulgarianWordEntry("молив", "✏️", List.of("МО", "ЛИВ")),
            new BulgarianWordEntry("зебра", "🦓", List.of("ЗЕБ", "РА")),
            new BulgarianWordEntry("панда", "🐼", List.of("ПАН", "ДА")),
            new BulgarianWordEntry("мечка", "🐻", List.of("МЕЧ", "КА")),
            new BulgarianWordEntry("ягода", "🍓", List.of("Я", "ГО", "ДА")),
            new BulgarianWordEntry("лимон", "🍋", List.of("ЛИ", "МОН")),
            new BulgarianWordEntry("банан", "🍌", List.of("БА", "НАН")),
            new BulgarianWordEntry("круша", "🍐", List.of("КРУ", "ША")),
            new BulgarianWordEntry("цвете", "🌼", List.of("ЦВЕ", "ТЕ")),
            new BulgarianWordEntry("ябълка", "🍎", List.of("Я", "БЪЛ", "КА")),
            new BulgarianWordEntry("морков", "🥕", List.of("МОР", "КОВ")),
            new BulgarianWordEntry("камила", "🐫", List.of("КА", "МИ", "ЛА")),
            new BulgarianWordEntry("грозде", "🍇", List.of("ГРОЗ", "ДЕ")),
            new BulgarianWordEntry("слънце", "☀️", List.of("СЛЪН", "ЦЕ")),
            new BulgarianWordEntry("ракета", "🚀", List.of("РА", "КЕ", "ТА")),
            new BulgarianWordEntry("обувка", "👟", List.of("О", "БУВ", "КА")),
            new BulgarianWordEntry("китара", "🎸", List.of("КИ", "ТА", "РА")),
            new BulgarianWordEntry("балон", "🎈", List.of("БА", "ЛОН")),
            new BulgarianWordEntry("звезда", "⭐", List.of("ЗВЕЗ", "ДА")),
            new BulgarianWordEntry("телефон", "☎️", List.of("ТЕ", "ЛЕ", "ФОН")),
            new BulgarianWordEntry("автобус", "🚌", List.of("АВ", "ТО", "БУС")),
            new BulgarianWordEntry("самолет", "✈️", List.of("СА", "МО", "ЛЕТ")),
            new BulgarianWordEntry("планина", "⛰️", List.of("ПЛА", "НИ", "НА")),
            new BulgarianWordEntry("кокошка", "🐔", List.of("КО", "КОШ", "КА")),
            new BulgarianWordEntry("камбана", "🔔", List.of("КАМ", "БА", "НА")),
            new BulgarianWordEntry("калинка", "🐞", List.of("КА", "ЛИН", "КА")),
            new BulgarianWordEntry("барабан", "🥁", List.of("БА", "РА", "БАН")),
            new BulgarianWordEntry("пингвин", "🐧", List.of("ПИН", "ГВИН")),
            new BulgarianWordEntry("пеперуда", "🦋", List.of("ПЕ", "ПЕ", "РУ", "ДА")),
            new BulgarianWordEntry("праскова", "🍑", List.of("ПРАС", "КО", "ВА")),
            new BulgarianWordEntry("портокал", "🍊", List.of("ПОР", "ТО", "КАЛ")),
            new BulgarianWordEntry("прозорец", "🪟", List.of("ПРО", "ЗО", "РЕЦ")),
            new BulgarianWordEntry("крокодил", "🐊", List.of("КРО", "КО", "ДИЛ")),
            new BulgarianWordEntry("светофар", "🚦", List.of("СВЕ", "ТО", "ФАР")),
            new BulgarianWordEntry("компютър", "💻", List.of("КОМ", "ПЮ", "ТЪР")),
            new BulgarianWordEntry("динозавър", "🦖", List.of("ДИ", "НО", "ЗА", "ВЪР")),
            new BulgarianWordEntry("телевизор", "📺", List.of("ТЕ", "ЛЕ", "ВИ", "ЗОР")),
            new BulgarianWordEntry("костенурка", "🐢", List.of("КОС", "ТЕ", "НУР", "КА")),
            new BulgarianWordEntry("хеликоптер", "🚁", List.of("ХЕ", "ЛИ", "КОП", "ТЕР")),
            new BulgarianWordEntry("слънчоглед", "🌻", List.of("СЛЪН", "ЧО", "ГЛЕД")),
            new BulgarianWordEntry("пожарникар", "🧑‍🚒", List.of("ПО", "ЖАР", "НИ", "КАР")),
            new BulgarianWordEntry("син кит", "🐋", List.of("СИН", "КИТ")),
            new BulgarianWordEntry("бял заек", "🐇", List.of("БЯЛ", "ЗА", "ЕК")),
            new BulgarianWordEntry("зелена жаба", "🐸", List.of("ЗЕ", "ЛЕ", "НА", "ЖА", "БА")),
            new BulgarianWordEntry("жълта звезда", "🌟", List.of("ЖЪЛ", "ТА", "ЗВЕЗ", "ДА")),
            new BulgarianWordEntry("червено сърце", "❤️", List.of("ЧЕР", "ВЕ", "НО", "СЪР", "ЦЕ")),
            new BulgarianWordEntry("черна котка", "🐈‍⬛", List.of("ЧЕР", "НА", "КОТ", "КА")),
            new BulgarianWordEntry("мама", "focus:👩|👧|0", List.of("МА", "МА"), 1),
            new BulgarianWordEntry("тате", "👨", List.of("ТА", "ТЕ"), 1),
            new BulgarianWordEntry("баба", "👵", List.of("БА", "БА"), 1),
            new BulgarianWordEntry("дядо", "👴", List.of("ДЯ", "ДО"), 1),
            new BulgarianWordEntry("кака", "focus:👧|👦|0", List.of("КА", "КА"), 1),
            new BulgarianWordEntry("бебе", "👶", List.of("БЕ", "БЕ"), 1),
            new BulgarianWordEntry("супа", "🍲", List.of("СУ", "ПА"), 2),
            new BulgarianWordEntry("торта", "🎂", List.of("ТОР", "ТА"), 3),
            new BulgarianWordEntry("бонбон", "🍬", List.of("БОН", "БОН"), 4),
            new BulgarianWordEntry("диня", "🍉", List.of("ДИ", "НЯ"), 2),
            new BulgarianWordEntry("череша", "🍒", List.of("ЧЕ", "РЕ", "ША"), 4),
            new BulgarianWordEntry("домат", "🍅", List.of("ДО", "МАТ"), 3),
            new BulgarianWordEntry("царевица", "🌽", List.of("ЦА", "РЕ", "ВИ", "ЦА"), 6),
            new BulgarianWordEntry("картоф", "🥔", List.of("КАР", "ТОФ"), 4),
            new BulgarianWordEntry("сладолед", "🍦", List.of("СЛА", "ДО", "ЛЕД"), 6),
            new BulgarianWordEntry("крава", "🐄", List.of("КРА", "ВА"), 3),
            new BulgarianWordEntry("овца", "🐑", List.of("ОВ", "ЦА"), 2),
            new BulgarianWordEntry("прасе", "🐷", List.of("ПРА", "СЕ"), 3),
            new BulgarianWordEntry("мишка", "🐭", List.of("МИШ", "КА"), 3),
            new BulgarianWordEntry("заек", "🐰", List.of("ЗА", "ЕК"), 2),
            new BulgarianWordEntry("тигър", "🐯", List.of("ТИ", "ГЪР"), 3),
            new BulgarianWordEntry("маймуна", "🐒", List.of("МАЙ", "МУ", "НА"), 5),
            new BulgarianWordEntry("жираф", "🦒", List.of("ЖИ", "РАФ"), 3),
            new BulgarianWordEntry("слон", "🐘", List.of("СЛОН"), 2),
            new BulgarianWordEntry("октопод", "🐙", List.of("ОК", "ТО", "ПОД"), 5),
            new BulgarianWordEntry("пчела", "🐝", List.of("ПЧЕ", "ЛА"), 3),
            new BulgarianWordEntry("мравка", "🐜", List.of("МРАВ", "КА"), 4),
            new BulgarianWordEntry("охлюв", "🐌", List.of("ОХ", "ЛЮВ"), 3),
            new BulgarianWordEntry("часовник", "⏰", List.of("ЧА", "СОВ", "НИК"), 6),
            new BulgarianWordEntry("лампа", "💡", List.of("ЛАМ", "ПА"), 3),
            new BulgarianWordEntry("къща", "🏡", List.of("КЪ", "ЩА"), 2),
            new BulgarianWordEntry("врата", "🚪", List.of("ВРА", "ТА"), 3),
            new BulgarianWordEntry("легло", "🛏️", List.of("ЛЕГ", "ЛО"), 3),
            new BulgarianWordEntry("диван", "🛋️", List.of("ДИ", "ВАН"), 3),
            new BulgarianWordEntry("чадър", "☂️", List.of("ЧА", "ДЪР"), 3),
            new BulgarianWordEntry("чанта", "🎒", List.of("ЧАН", "ТА"), 3),
            new BulgarianWordEntry("ножица", "✂️", List.of("НО", "ЖИ", "ЦА"), 5),
            new BulgarianWordEntry("фенер", "🔦", List.of("ФЕ", "НЕР"), 3),
            new BulgarianWordEntry("кораб", "🚢", List.of("КО", "РАБ"), 3),
            new BulgarianWordEntry("лодка", "⛵", List.of("ЛОД", "КА"), 3),
            new BulgarianWordEntry("трактор", "🚜", List.of("ТРАК", "ТОР"), 5),
            new BulgarianWordEntry("мотор", "🏍️", List.of("МО", "ТОР"), 3),
            new BulgarianWordEntry("облак", "☁️", List.of("ОБ", "ЛАК"), 3),
            new BulgarianWordEntry("дъжд", "🌧️", List.of("ДЪЖД"), 2),
            new BulgarianWordEntry("сняг", "❄️", List.of("СНЯГ"), 2),
            new BulgarianWordEntry("река", "🏞️", List.of("РЕ", "КА"), 2),
            new BulgarianWordEntry("море", "🌊", List.of("МО", "РЕ"), 2),
            new BulgarianWordEntry("гора", "🌲🌳🌲", List.of("ГО", "РА"), 2),
            new BulgarianWordEntry("листо", "🍃", List.of("ЛИС", "ТО"), 3),
            new BulgarianWordEntry("камък", "🪨", List.of("КА", "МЪК"), 3),
            new BulgarianWordEntry("огън", "🔥", List.of("О", "ГЪН"), 2),
            new BulgarianWordEntry("дъга", "🌈", List.of("ДЪ", "ГА"), 2),
            new BulgarianWordEntry("лекар", "🧑‍⚕️", List.of("ЛЕ", "КАР"), 3),
            new BulgarianWordEntry("готвач", "🧑‍🍳", List.of("ГОТ", "ВАЧ"), 4),
            new BulgarianWordEntry("полицай", "👮", List.of("ПО", "ЛИ", "ЦАЙ"), 5),
            new BulgarianWordEntry("учител", "👩‍🏫", List.of("У", "ЧИ", "ТЕЛ"), 4),
            new BulgarianWordEntry("музика", "🎵", List.of("МУ", "ЗИ", "КА"), 4),
            new BulgarianWordEntry("пиано", "🎹", List.of("ПИ", "А", "НО"), 3),
            new BulgarianWordEntry("цигулка", "🎻", List.of("ЦИ", "ГУЛ", "КА"), 5),
            new BulgarianWordEntry("микрофон", "🎤", List.of("МИК", "РО", "ФОН"), 6),
            new BulgarianWordEntry("корона", "👑", List.of("КО", "РО", "НА"), 4),
            new BulgarianWordEntry("подарък", "🎁", List.of("ПО", "ДА", "РЪК"), 5),
            new BulgarianWordEntry("пъзел", "🧩", List.of("ПЪ", "ЗЕЛ"), 3),
            new BulgarianWordEntry("кубче", "🎲", List.of("КУБ", "ЧЕ"), 3),
            new BulgarianWordEntry("робот", "🤖", List.of("РО", "БОТ"), 3),
            new BulgarianWordEntry("магнит", "🧲", List.of("МАГ", "НИТ"), 4),
            new BulgarianWordEntry("очила", "👓", List.of("О", "ЧИ", "ЛА"), 4),
            new BulgarianWordEntry("пръстен", "💍", List.of("ПРЪС", "ТЕН"), 5),
            new BulgarianWordEntry("ръкавица", "🧤", List.of("РЪ", "КА", "ВИ", "ЦА"), 6),
            new BulgarianWordEntry("палто", "🧥", List.of("ПАЛ", "ТО"), 3),
            new BulgarianWordEntry("шал", "🧣", List.of("ШАЛ"), 1),
            new BulgarianWordEntry("шапка", "🎩", List.of("ШАП", "КА"), 3),
            new BulgarianWordEntry("чорап", "🧦", List.of("ЧО", "РАП"), 3),
            new BulgarianWordEntry("бански", "🩱", List.of("БАН", "СКИ"), 4),
            new BulgarianWordEntry("ключалка", "🔒", List.of("КЛЮ", "ЧАЛ", "КА"), 6),
            new BulgarianWordEntry("писмо", "✉️", List.of("ПИС", "МО"), 3),
            new BulgarianWordEntry("плик", "📩", List.of("ПЛИК"), 2),
            new BulgarianWordEntry("вестник", "📰", List.of("ВЕСТ", "НИК"), 5),
            new BulgarianWordEntry("поща", "📮", List.of("ПО", "ЩА"), 3),
            new BulgarianWordEntry("кутия", "📦", List.of("КУ", "ТИ", "Я"), 4),
            new BulgarianWordEntry("пазар", "🛒", List.of("ПА", "ЗАР"), 3),
            new BulgarianWordEntry("пари", "💰", List.of("ПА", "РИ"), 2),
            new BulgarianWordEntry("карта", "🗺️", List.of("КАР", "ТА"), 3),
            new BulgarianWordEntry("компас", "🧭", List.of("КОМ", "ПАС"), 4),
            new BulgarianWordEntry("палатка", "⛺", List.of("ПА", "ЛАТ", "КА"), 5),
            new BulgarianWordEntry("вулкан", "🌋", List.of("ВУЛ", "КАН"), 4),
            new BulgarianWordEntry("остров", "🏝️", List.of("ОС", "ТРОВ"), 4),
            new BulgarianWordEntry("плаж", "🏖️", List.of("ПЛАЖ"), 2),
            new BulgarianWordEntry("футбол", "🥅", List.of("ФУТ", "БОЛ"), 4),
            new BulgarianWordEntry("баскетбол", "🏀", List.of("БАС", "КЕТ", "БОЛ"), 7),
            new BulgarianWordEntry("медал", "🏅", List.of("МЕ", "ДАЛ"), 3),
            new BulgarianWordEntry("купа", "🏆", List.of("КУ", "ПА"), 2),
            new BulgarianWordEntry("мишена", "🎯", List.of("МИ", "ШЕ", "НА"), 4),
            new BulgarianWordEntry("картина", "🖼️", List.of("КАР", "ТИ", "НА"), 5),
            new BulgarianWordEntry("четка", "🖌️", List.of("ЧЕТ", "КА"), 3),
            new BulgarianWordEntry("боя", "🎨", List.of("БО", "Я"), 2),
            new BulgarianWordEntry("театър", "🎭", List.of("ТЕ", "А", "ТЪР"), 4),
            new BulgarianWordEntry("цирк", "🎪", List.of("ЦИРК"), 2),
            new BulgarianWordEntry("билет", "🎟️", List.of("БИ", "ЛЕТ"), 3),
            new BulgarianWordEntry("сцена", "🎬", List.of("СЦЕ", "НА"), 3),
            new BulgarianWordEntry("светкавица", "⚡", List.of("СВЕТ", "КА", "ВИ", "ЦА"), 7),
            new BulgarianWordEntry("планета", "🪐", List.of("ПЛА", "НЕ", "ТА"), 5),
            new BulgarianWordEntry("луна и звезда", "🌙⭐", List.of("ЛУ", "НА", "И", "ЗВЕЗ", "ДА"), 9),
            new BulgarianWordEntry("синя кола", "🚙", List.of("СИ", "НЯ", "КО", "ЛА"), 7),
            new BulgarianWordEntry("малка лодка", "🛶", List.of("МАЛ", "КА", "ЛОД", "КА"), 8)
    );
}
