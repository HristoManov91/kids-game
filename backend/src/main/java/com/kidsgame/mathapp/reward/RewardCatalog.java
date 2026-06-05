package com.kidsgame.mathapp.reward;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RewardCatalog {
    private final Map<String, RewardThemeResponse> themes;
    private final Map<String, RewardCatalogItemResponse> items;
    private final RewardCatalogItemRepository itemRepository;

    public RewardCatalog() {
        this(null);
    }

    @Autowired
    public RewardCatalog(RewardCatalogItemRepository itemRepository) {
        this.themes = new LinkedHashMap<>();
        this.items = new LinkedHashMap<>();
        this.itemRepository = itemRepository;
        seedThemes();
        seedItems();
    }

    @PostConstruct
    void seedPersistentCatalog() {
        if (itemRepository == null) {
            return;
        }
        for (RewardCatalogItemResponse item : items.values()) {
            if (!itemRepository.existsById(item.id())) {
                itemRepository.save(new RewardCatalogItemEntity(item));
            }
        }
    }

    public RewardCatalogResponse catalog() {
        return new RewardCatalogResponse(List.copyOf(themes.values()), activeItems());
    }

    public RewardThemeResponse theme(String themeId) {
        RewardThemeResponse theme = themes.get(themeId);
        if (theme == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Избери свят за картината.");
        }
        return theme;
    }

    public RewardCatalogItemResponse item(String itemId) {
        if (itemRepository != null) {
            return itemRepository.findById(itemId)
                    .filter(RewardCatalogItemEntity::isActive)
                    .map(RewardCatalogItemEntity::toResponse)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Този предмет не е в каталога."));
        }
        RewardCatalogItemResponse item = items.get(itemId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Този предмет не е в каталога.");
        }
        return item;
    }

    public List<RewardCatalogItemResponse> itemsForTheme(String themeId) {
        return activeItems().stream()
                .filter(item -> item.belongsToTheme(themeId))
                .toList();
    }

    private List<RewardCatalogItemResponse> activeItems() {
        if (itemRepository == null) {
            return List.copyOf(items.values());
        }
        return itemRepository.findByActiveTrueOrderByThemeIdAscCategoryAscNameAsc()
                .stream()
                .map(RewardCatalogItemEntity::toResponse)
                .toList();
    }

    private void seedThemes() {
        addTheme("forest-meadow", "Горска полянка", "Мека трева, дървета и приятели.",
                "linear-gradient(180deg, #b9e8ff 0%, #eaf8d7 45%, #8bd477 100%)", "/reward-assets/polished/farm/apple-tree.png", "Моята горска полянка");
        addTheme("space", "Космос", "Планети, звезди и далечни галактики.",
                "url('/api/reward-assets/static-space-space-background') center/cover no-repeat", "🚀", "Моят космос");
        addTheme("underwater", "Морско дъно", "Синя вода, рибки и съкровища.",
                "url('/api/reward-assets/static-sea-sea-background') center/cover no-repeat", "🐠", "Моето морско дъно");
        addTheme("farm", "Ферма", "Животни, градинка и хамбар.",
                "url('/api/reward-assets/static-farm-farm-background') center/cover no-repeat", "🚜", "Моята ферма");
        addTheme("jungle", "Джунгла", "Големи листа, лиани и животни.",
                "url('/api/reward-assets/static-jungle-jungle-background') center/cover no-repeat", "🦜", "Моята джунгла");
    }

    private void seedItems() {
        addItem("forest-tree", "forest-meadow", "Природа", "Дърво", 30, "/reward-assets/polished/farm/apple-tree.png", 1.2, 0.6, 2.4);
        addItem("forest-small-tree", "forest-meadow", "Природа", "Бор", 20, "🌲", 0.95, 0.5, 2.0);
        addItem("forest-red-flower", "forest-meadow", "Природа", "Червено цвете", 12, "/reward-assets/polished/forest/flower-red.png", 0.75, 0.35, 1.4);
        addItem("forest-yellow-flower", "forest-meadow", "Природа", "Жълто цвете", 12, "/reward-assets/polished/forest/flower-yellow.png", 0.75, 0.35, 1.45);
        addItem("forest-purple-flower", "forest-meadow", "Природа", "Лилаво цвете", 12, "/reward-assets/polished/forest/flower-purple.png", 0.75, 0.35, 1.4);
        addItem("forest-rock", "forest-meadow", "Предмети", "Камък", 10, "/reward-assets/polished/forest/forest-rocks.png", 0.75, 0.4, 1.6);
        addItem("forest-path", "forest-meadow", "Пътеки", "Пътечка", 25, "🟫", 1.5, 0.7, 3.0);
        addItem("forest-bench", "forest-meadow", "Предмети", "Пейка", 40, "/reward-assets/polished/forest/forest-bench.png", 1.0, 0.5, 2.0);
        addItem("forest-swing", "forest-meadow", "Предмети", "Люлка", 45, "/reward-assets/polished/forest/forest-swing.png", 1.05, 0.5, 2.2);
        addItem("forest-pond", "forest-meadow", "Природа", "Езерце", 35, "/reward-assets/polished/forest/forest-pond.png", 1.2, 0.55, 2.6);
        addItem("forest-picnic-blanket", "forest-meadow", "Предмети", "Одеяло за пикник", 35, "/reward-assets/forest/picnic-blanket.svg", 1.25, 0.6, 2.4);
        addItem("forest-cabin", "forest-meadow", "Предмети", "Горска хижа", 110, "/reward-assets/forest/forest-cabin.svg", 1.4, 0.7, 2.7);
        addItem("forest-birdhouse", "forest-meadow", "Украси", "Къщичка за птици", 50, "🏠", 0.85, 0.45, 1.8);
        addItem("forest-sun", "forest-meadow", "Небе", "Слънце", 30, "☀️", 0.9, 0.45, 1.9);
        addItem("forest-cloud", "forest-meadow", "Небе", "Облак", 25, "☁️", 0.95, 0.45, 2.0);
        addItem("forest-rainbow", "forest-meadow", "Небе", "Дъга", 55, "🌈", 1.2, 0.55, 2.4);
        addItem("forest-kid", "forest-meadow", "Хора", "Дете", 55, "/reward-assets/forest/child.svg", 0.9, 0.45, 1.8);
        addItem("forest-girl", "forest-meadow", "Хора", "Момиче", 55, "/reward-assets/forest/girl.svg", 0.9, 0.45, 1.8);
        addItem("forest-boy", "forest-meadow", "Хора", "Момче", 55, "/reward-assets/forest/boy.svg", 0.9, 0.45, 1.8);
        addItem("forest-running-kid", "forest-meadow", "Хора", "Тичащо дете", 60, "/reward-assets/forest/running-child.svg", 0.9, 0.45, 1.8);
        addItem("forest-ball", "forest-meadow", "Предмети", "Топка", 25, "⚽", 0.65, 0.35, 1.5);
        addItem("forest-orange-ball", "forest-meadow", "Предмети", "Баскетболна топка", 25, "🏀", 0.65, 0.35, 1.5);
        addItem("forest-volleyball-net", "forest-meadow", "Предмети", "Волейболна мрежа", 70, "/reward-assets/forest/volleyball-net.svg", 1.4, 0.7, 2.8);
        addItem("forest-football-goal", "forest-meadow", "Предмети", "Футболна врата", 75, "/reward-assets/forest/football-goal.svg", 1.35, 0.7, 2.8);
        addItem("forest-basketball-hoop", "forest-meadow", "Предмети", "Баскетболен кош", 75, "/reward-assets/forest/basketball-hoop.svg", 1.2, 0.6, 2.4);
        addItem("forest-bunny", "forest-meadow", "Животни", "Зайче", 60, "/reward-assets/polished/forest/bunny.png", 0.85, 0.45, 1.8);
        addItem("forest-dog", "forest-meadow", "Животни", "Куче", 50, "/reward-assets/polished/forest/dog.png", 0.85, 0.45, 1.8);
        addItem("forest-cat", "forest-meadow", "Животни", "Котка", 50, "🐈", 0.85, 0.45, 1.8);
        addItem("forest-bear", "forest-meadow", "Животни", "Мечка", 95, "/reward-assets/polished/forest/bear.png", 1.0, 0.5, 2.1);
        addItem("forest-fox", "forest-meadow", "Животни", "Лисица", 70, "/reward-assets/polished/forest/fox.png", 0.9, 0.45, 1.9);
        addItem("forest-wolf", "forest-meadow", "Животни", "Вълк", 85, "/reward-assets/polished/forest/wolf.png", 1.0, 0.5, 2.1);
        addItem("forest-hedgehog", "forest-meadow", "Животни", "Таралеж", 45, "/reward-assets/polished/forest/hedgehog.png", 0.72, 0.35, 1.55);
        addItem("forest-horse", "forest-meadow", "Животни", "Кон", 80, "/reward-assets/polished/forest/horse.png", 1.05, 0.5, 2.25);
        addItem("forest-donkey", "forest-meadow", "Животни", "Магаре", 70, "/reward-assets/polished/forest/donkey.png", 0.98, 0.48, 2.05);
        addItem("forest-deer", "forest-meadow", "Животни", "Елен", 85, "/reward-assets/polished/forest/deer.png", 1.0, 0.5, 2.1);
        addItem("forest-doe", "forest-meadow", "Животни", "Сърна", 75, "/reward-assets/polished/forest/doe.png", 0.95, 0.5, 2.0);
        addItem("forest-fawn", "forest-meadow", "Животни", "Малка сърна", 70, "/reward-assets/polished/forest/fawn.png", 0.78, 0.42, 1.8);
        addItem("forest-boar", "forest-meadow", "Животни", "Диво прасе", 70, "/reward-assets/polished/forest/boar.png", 0.9, 0.45, 1.9);
        addItem("forest-squirrel", "forest-meadow", "Животни", "Катеричка", 60, "/reward-assets/polished/forest/squirrel.png", 0.85, 0.45, 1.8);
        addItem("forest-butterfly", "forest-meadow", "Животни", "Пеперуда", 35, "/reward-assets/polished/forest/butterfly.png", 0.65, 0.35, 1.4);
        addItem("forest-ladybug", "forest-meadow", "Животни", "Калинка", 20, "/reward-assets/polished/forest/ladybug.png", 0.55, 0.3, 1.25);
        addItem("forest-bee", "forest-meadow", "Животни", "Пчела", 25, "🐝", 0.58, 0.3, 1.3);
        addItem("forest-bird", "forest-meadow", "Животни", "Птичка", 45, "/reward-assets/polished/forest/bird.png", 0.65, 0.35, 1.4);
        addItem("forest-chick", "forest-meadow", "Животни", "Пиленце", 35, "/reward-assets/polished/forest/chick.png", 0.62, 0.35, 1.35);
        addItem("forest-black-bird", "forest-meadow", "Животни", "Черна птица", 45, "/reward-assets/polished/forest/black-bird.png", 0.65, 0.35, 1.4);
        addItem("forest-woodpecker", "forest-meadow", "Животни", "Кълвач", 50, "/reward-assets/polished/forest/woodpecker.png", 0.65, 0.35, 1.4);
        addItem("forest-apple", "forest-meadow", "Храна", "Ябълка", 10, "🍎", 0.55, 0.3, 1.25);
        addItem("forest-banana", "forest-meadow", "Храна", "Банан", 12, "🍌", 0.6, 0.3, 1.3);
        addItem("forest-orange", "forest-meadow", "Храна", "Портокал", 12, "🍊", 0.58, 0.3, 1.3);
        addItem("forest-sandwich", "forest-meadow", "Храна", "Сандвич", 15, "🥪", 0.65, 0.35, 1.4);
        addItem("forest-croissant", "forest-meadow", "Храна", "Кроасан", 15, "🥐", 0.65, 0.35, 1.4);
        addItem("forest-watermelon", "forest-meadow", "Храна", "Диня", 20, "🍉", 0.7, 0.35, 1.5);
        addItem("forest-juice", "forest-meadow", "Храна", "Сок", 15, "🧃", 0.65, 0.35, 1.4);
        addItem("forest-cupcake", "forest-meadow", "Храна", "Кексче", 20, "🧁", 0.62, 0.35, 1.4);
        addItem("forest-cake", "forest-meadow", "Храна", "Торта", 35, "🎂", 0.8, 0.4, 1.7);
        addItem("forest-breadsticks", "forest-meadow", "Храна", "Франзела", 15, "🥖", 0.65, 0.35, 1.4);
        addItem("forest-grapes", "forest-meadow", "Храна", "Грозде", 15, "🍇", 0.65, 0.35, 1.4);
        addItem("forest-cookies", "forest-meadow", "Храна", "Бисквити", 15, "🍪", 0.62, 0.35, 1.4);
        addItem("forest-picnic-basket", "forest-meadow", "Храна", "Кошница", 30, "🧺", 0.75, 0.4, 1.6);

        addItem("space-star", "space", "Космос", "Звезда", 10, "/reward-assets/space/star.svg", 0.7, 0.35, 1.6);
        addItem("space-sun", "space", "Космос", "Слънце", 80, "/reward-assets/space/sun.svg", 1.15, 0.55, 2.4);
        addItem("space-planet", "space", "Планети", "Планета", 40, "/reward-assets/space/planet.svg", 1.0, 0.5, 2.2);
        addItem("space-mercury", "space", "Планети", "Меркурий", 25, "/reward-assets/space/mercury.svg", 0.75, 0.35, 1.6);
        addItem("space-venus", "space", "Планети", "Венера", 30, "/reward-assets/space/venus.svg", 0.85, 0.4, 1.8);
        addItem("space-earth", "space", "Планети", "Земя", 40, "/reward-assets/space/earth.svg", 0.9, 0.45, 1.9);
        addItem("space-mars", "space", "Планети", "Марс", 35, "/reward-assets/space/mars.svg", 0.85, 0.4, 1.8);
        addItem("space-jupiter", "space", "Планети", "Юпитер", 65, "/reward-assets/space/jupiter.svg", 1.15, 0.55, 2.5);
        addItem("space-saturn", "space", "Планети", "Сатурн", 65, "/reward-assets/space/saturn.svg", 1.2, 0.55, 2.6);
        addItem("space-uranus", "space", "Планети", "Уран", 50, "/reward-assets/space/uranus.svg", 1.0, 0.5, 2.2);
        addItem("space-neptune", "space", "Планети", "Нептун", 50, "/reward-assets/space/neptune.svg", 1.0, 0.5, 2.2);
        addItem("space-moon", "space", "Космос", "Луна", 25, "/reward-assets/space/moon.svg", 0.75, 0.35, 1.6);
        addItem("space-comet", "space", "Космос", "Комета", 45, "/reward-assets/space/comet.svg", 1.0, 0.45, 2.1);
        addItem("space-asteroid", "space", "Космос", "Астероид", 20, "/reward-assets/space/asteroid.svg", 0.75, 0.35, 1.6);
        addItem("space-rocket", "space", "Кораби", "Ракета", 100, "/reward-assets/space/rocket.svg", 1.0, 0.5, 2.2);
        addItem("space-astronaut", "space", "Герои", "Космонавт", 120, "/reward-assets/space/astronaut.svg", 1.0, 0.5, 2.2);
        addItem("space-robot", "space", "Герои", "Робот", 80, "/reward-assets/space/robot.svg", 0.95, 0.5, 2.0);
        addItem("space-satellite", "space", "Кораби", "Сателит", 90, "/reward-assets/space/satellite.svg", 0.95, 0.45, 2.0);
        addItem("space-station", "space", "Станции", "Космическа станция", 130, "/reward-assets/space/space-station.svg", 1.25, 0.6, 2.7);
        addItem("space-milky-way", "space", "Галактики", "Млечен път", 100, "/reward-assets/space/milky-way.svg", 1.35, 0.65, 2.8);
        addItem("space-ufo", "space", "Кораби", "НЛО кораб", 90, "/reward-assets/space/ufo.svg", 1.05, 0.5, 2.2);
        addItem("space-black-hole", "space", "Галактики", "Черна дупка", 110, "/reward-assets/space/black-hole.svg", 1.15, 0.55, 2.4);
        addItem("space-nebula", "space", "Галактики", "Мъглявина", 80, "/reward-assets/space/nebula.svg", 1.2, 0.55, 2.5);
        addItem("space-rover", "space", "Кораби", "Марсоход", 95, "/reward-assets/space/rover.svg", 1.0, 0.5, 2.1);
        addItem("space-telescope", "space", "Кораби", "Космически телескоп", 100, "/reward-assets/space/telescope.svg", 1.0, 0.5, 2.2);
        addItem("space-alien", "space", "Герои", "Извънземно", 75, "/reward-assets/space/alien.svg", 0.9, 0.45, 1.9);
        addItem("space-constellation", "space", "Галактики", "Съзвездие", 55, "/reward-assets/space/constellation.svg", 1.0, 0.5, 2.2);

        addItem("sea-fish", "underwater", "Риби", "Синя рибка", 30, "/reward-assets/sea/fish-blue.svg", 0.85, 0.40, 1.80);
        addItem("sea-yellow-fish", "underwater", "Риби", "Жълта рибка", 30, "/reward-assets/sea/fish-yellow.svg", 0.85, 0.40, 1.80);
        addItem("sea-red-fish", "underwater", "Риби", "Червена рибка", 30, "/reward-assets/sea/fish-red.svg", 0.85, 0.40, 1.80);
        addItem("sea-purple-fish", "underwater", "Риби", "Лилава рибка", 30, "/reward-assets/sea/fish-purple.svg", 0.85, 0.40, 1.80);
        addItem("sea-goldfish", "underwater", "Риби", "Златна рибка", 40, "/reward-assets/sea/goldfish.svg", 0.85, 0.40, 1.80);
        addItem("sea-clownfish", "underwater", "Риби", "Рибка клоун", 45, "/reward-assets/sea/clownfish.svg", 0.90, 0.42, 1.90);
        addItem("sea-dolphin", "underwater", "Морски животни", "Делфин", 100, "/reward-assets/sea/dolphin.svg", 1.15, 0.55, 2.45);
        addItem("sea-whale", "underwater", "Морски животни", "Кит", 120, "/reward-assets/sea/whale.svg", 1.25, 0.60, 2.70);
        addItem("sea-shark", "underwater", "Морски животни", "Акула", 110, "/reward-assets/sea/shark.svg", 1.20, 0.60, 2.55);
        addItem("sea-turtle", "underwater", "Морски животни", "Костенурка", 80, "/reward-assets/polished/sea/turtle.png", 0.95, 0.45, 2.00);
        addItem("sea-stingray", "underwater", "Морски животни", "Скат", 85, "/reward-assets/sea/stingray.svg", 1.05, 0.50, 2.25);
        addItem("sea-octopus", "underwater", "Морски животни", "Октопод", 85, "/reward-assets/sea/octopus.svg", 0.95, 0.45, 2.00);
        addItem("sea-jellyfish", "underwater", "Морски животни", "Медуза", 60, "/reward-assets/sea/jellyfish.svg", 0.95, 0.45, 2.00);
        addItem("sea-seahorse-yellow", "underwater", "Морски животни", "Жълто морско конче", 55, "/reward-assets/polished/sea/seahorse-yellow.svg", 0.90, 0.42, 1.95);
        addItem("sea-seahorse-purple", "underwater", "Морски животни", "Лилаво морско конче", 55, "/reward-assets/polished/sea/seahorse-purple.svg", 0.90, 0.42, 1.95);
        addItem("sea-crab", "underwater", "Морски животни", "Червен рак", 40, "/reward-assets/sea/crab-red.svg", 0.85, 0.40, 1.85);
        addItem("sea-blue-crab", "underwater", "Морски животни", "Син рак", 40, "/reward-assets/sea/crab-blue.svg", 0.85, 0.40, 1.85);
        addItem("sea-star", "underwater", "Морско дъно", "Оранжева морска звезда", 20, "/reward-assets/sea/starfish-orange.svg", 0.70, 0.35, 1.60);
        addItem("sea-pink-starfish", "underwater", "Морско дъно", "Розова морска звезда", 20, "/reward-assets/sea/starfish-pink.svg", 0.70, 0.35, 1.60);
        addItem("sea-seaweed", "underwater", "Растения", "Високи водорасли", 15, "/reward-assets/sea/seaweed-tall.svg", 0.95, 0.45, 2.05);
        addItem("sea-curly-seaweed", "underwater", "Растения", "Къдрави водорасли", 15, "/reward-assets/sea/seaweed-curly.svg", 0.95, 0.45, 2.05);
        addItem("sea-coral", "underwater", "Корали", "Червен корал", 30, "/reward-assets/sea/coral-red.svg", 0.95, 0.45, 2.05);
        addItem("sea-purple-coral", "underwater", "Корали", "Лилав корал", 30, "/reward-assets/sea/coral-purple.svg", 0.95, 0.45, 2.05);
        addItem("sea-fan-coral", "underwater", "Корали", "Ветрило корал", 35, "/reward-assets/sea/coral-fan.svg", 1.00, 0.45, 2.10);
        addItem("sea-shell", "underwater", "Морско дъно", "Мида", 20, "/reward-assets/sea/shell.svg", 0.70, 0.35, 1.50);
        addItem("sea-rapan", "underwater", "Морско дъно", "Рапан", 20, "/reward-assets/sea/rapan.svg", 0.75, 0.35, 1.60);
        addItem("sea-pearl-shell", "underwater", "Морско дъно", "Мида с перла", 35, "/reward-assets/sea/pearl-shell.svg", 0.75, 0.35, 1.60);
        addItem("sea-treasure", "underwater", "Съкровища", "Съкровище", 120, "/reward-assets/sea/treasure-chest.svg", 1.00, 0.50, 2.20);
        addItem("sea-anchor", "underwater", "Съкровища", "Котва", 45, "/reward-assets/sea/anchor.svg", 0.95, 0.45, 2.00);
        addItem("sea-bubbles", "underwater", "Украси", "Мехурчета", 10, "/reward-assets/sea/bubbles.svg", 0.90, 0.40, 1.90);
        addItem("sea-submarine", "underwater", "Предмети", "Подводница", 100, "/reward-assets/sea/submarine.svg", 1.15, 0.55, 2.45);

        addItem("farm-sheep-ewe", "farm", "Животни", "Овца", 55, "/reward-assets/polished/farm/sheep-ewe.png", 0.95, 0.45, 2.05);
        addItem("farm-sheep-ram", "farm", "Животни", "Овен", 60, "/reward-assets/polished/farm/sheep-ram.png", 1.00, 0.45, 2.15);
        addItem("farm-lamb", "farm", "Животни", "Агънце", 40, "/reward-assets/polished/farm/lamb.png", 0.75, 0.35, 1.60);
        addItem("farm-goat-doe", "farm", "Животни", "Коза", 55, "/reward-assets/polished/farm/goat-doe.png", 0.95, 0.45, 2.05);
        addItem("farm-goat-buck", "farm", "Животни", "Козел", 60, "/reward-assets/polished/farm/goat-buck.png", 1.00, 0.45, 2.15);
        addItem("farm-kid-goat", "farm", "Животни", "Яре", 40, "/reward-assets/polished/farm/kid-goat.png", 0.75, 0.35, 1.60);
        addItem("farm-hen-white", "farm", "Животни", "Бяла кокошка", 35, "/reward-assets/polished/farm/hen-white.png", 0.78, 0.35, 1.65);
        addItem("farm-hen-brown", "farm", "Животни", "Кафява кокошка", 35, "/reward-assets/polished/farm/hen-brown.png", 0.78, 0.35, 1.65);
        addItem("farm-rooster", "farm", "Животни", "Петел", 45, "/reward-assets/polished/farm/rooster.png", 0.85, 0.40, 1.85);
        addItem("farm-chick", "farm", "Животни", "Пиленце", 25, "/reward-assets/polished/farm/chick.png", 0.60, 0.30, 1.35);
        addItem("farm-duck", "farm", "Животни", "Патица", 40, "/reward-assets/polished/farm/duck.png", 0.82, 0.38, 1.80);
        addItem("farm-duckling", "farm", "Животни", "Патенце", 25, "/reward-assets/polished/farm/duckling.png", 0.60, 0.30, 1.35);
        addItem("farm-goose", "farm", "Животни", "Гъска", 45, "/reward-assets/polished/farm/goose.png", 0.90, 0.40, 1.90);
        addItem("farm-gosling", "farm", "Животни", "Гъсенце", 30, "/reward-assets/polished/farm/gosling.png", 0.62, 0.30, 1.40);
        addItem("farm-cow", "farm", "Животни", "Крава", 80, "/reward-assets/polished/farm/cow.png", 1.10, 0.55, 2.35);
        addItem("farm-bull", "farm", "Животни", "Бик", 85, "/reward-assets/polished/farm/bull.png", 1.15, 0.55, 2.45);
        addItem("farm-calf", "farm", "Животни", "Теле", 55, "/reward-assets/polished/farm/calf.png", 0.85, 0.40, 1.85);
        addItem("farm-horse", "farm", "Животни", "Кон", 80, "/reward-assets/polished/farm/horse.png", 1.10, 0.55, 2.35);
        addItem("farm-mare", "farm", "Животни", "Кобила", 80, "/reward-assets/polished/farm/mare.png", 1.08, 0.55, 2.30);
        addItem("farm-foal", "farm", "Животни", "Жребче", 55, "/reward-assets/polished/farm/foal.png", 0.82, 0.40, 1.80);
        addItem("farm-pig-sow", "farm", "Животни", "Прасе майка", 60, "/reward-assets/polished/farm/pig-sow.png", 1.00, 0.45, 2.10);
        addItem("farm-pig-boar", "farm", "Животни", "Глиган", 65, "/reward-assets/polished/farm/pig-boar.png", 1.05, 0.50, 2.20);
        addItem("farm-piglet", "farm", "Животни", "Прасенце", 35, "/reward-assets/polished/farm/piglet.png", 0.70, 0.35, 1.55);
        addItem("farm-barn", "farm", "Сгради", "Хамбар", 120, "/reward-assets/polished/farm/barn.png", 1.35, 0.65, 2.70);
        addItem("farm-stable", "farm", "Сгради", "Конюшня", 110, "/reward-assets/polished/farm/stable.png", 1.35, 0.65, 2.70);
        addItem("farm-chicken-coop", "farm", "Сгради", "Кокошарник", 90, "/reward-assets/polished/farm/chicken-coop.png", 1.15, 0.55, 2.35);
        addItem("farm-house", "farm", "Сгради", "Фермерска къща", 130, "/reward-assets/polished/farm/farm-house.png", 1.35, 0.65, 2.70);
        addItem("farm-silo", "farm", "Сгради", "Силоз", 95, "/reward-assets/polished/farm/silo.png", 1.15, 0.55, 2.35);
        addItem("farm-well", "farm", "Предмети", "Кладенец", 55, "/reward-assets/polished/farm/well.png", 0.95, 0.45, 2.00);
        addItem("farm-fence", "farm", "Предмети", "Ограда", 25, "/reward-assets/polished/farm/fence.png", 1.15, 0.55, 2.50);
        addItem("farm-gate", "farm", "Предмети", "Порта", 35, "/reward-assets/polished/farm/gate.png", 1.05, 0.50, 2.30);
        addItem("farm-farmer", "farm", "Хора", "Фермер", 70, "/reward-assets/polished/farm/farmer.png", 0.95, 0.45, 1.95);
        addItem("farm-farmer-woman", "farm", "Хора", "Фермерка", 70, "/reward-assets/polished/farm/farmer-woman.png", 0.95, 0.45, 1.95);
        addItem("farm-child-farmer", "farm", "Хора", "Малък помощник", 55, "/reward-assets/polished/farm/child-farmer.png", 0.82, 0.40, 1.75);
        addItem("farm-tractor", "farm", "Машини", "Трактор", 130, "/reward-assets/polished/farm/tractor.png", 1.25, 0.60, 2.65);
        addItem("farm-hay-bale", "farm", "Предмети", "Бала сено", 25, "/reward-assets/polished/farm/hay-bale.png", 0.95, 0.45, 2.00);
        addItem("farm-hay-stack", "farm", "Предмети", "Купа сено", 35, "/reward-assets/polished/farm/hay-stack.png", 1.05, 0.50, 2.25);
        addItem("farm-cart", "farm", "Предмети", "Дървена количка", 45, "/reward-assets/polished/farm/cart.png", 1.00, 0.50, 2.15);
        addItem("farm-watering-can", "farm", "Предмети", "Лейка", 25, "/reward-assets/polished/farm/watering-can.png", 0.80, 0.38, 1.75);
        addItem("farm-feed-bucket", "farm", "Предмети", "Кофа с храна", 25, "/reward-assets/polished/farm/feed-bucket.png", 0.82, 0.38, 1.75);
        addItem("farm-apple-tree", "farm", "Природа", "Ябълково дърво", 55, "/reward-assets/polished/farm/apple-tree.png", 1.10, 0.55, 2.35);
        addItem("farm-corn", "farm", "Растения", "Царевица", 20, "/reward-assets/polished/farm/corn.png", 0.85, 0.40, 1.85);
        addItem("farm-vegetable-bed", "farm", "Растения", "Зеленчукова леха", 35, "/reward-assets/polished/farm/vegetable-bed.png", 1.10, 0.55, 2.35);
        addItem("farm-sunflower", "farm", "Растения", "Слънчоглед", 18, "/reward-assets/polished/farm/sunflower.png", 0.85, 0.40, 1.80);

        addItem("jungle-palm-tree", "jungle", "Растения", "Палма", 45, "/reward-assets/jungle/palm-tree.svg", 1.10, 0.55, 2.35);
        addItem("jungle-banana-tree", "jungle", "Растения", "Бананово дърво", 55, "/reward-assets/jungle/banana-tree.svg", 1.10, 0.55, 2.35);
        addItem("jungle-giant-leaf", "jungle", "Растения", "Голямо листо", 20, "/reward-assets/jungle/giant-leaf.svg", 0.90, 0.45, 2.00);
        addItem("jungle-fern", "jungle", "Растения", "Папрат", 25, "/reward-assets/jungle/fern.svg", 0.95, 0.45, 2.05);
        addItem("jungle-liana", "jungle", "Растения", "Лиана", 30, "/reward-assets/jungle/liana.svg", 1.05, 0.50, 2.20);
        addItem("jungle-red-flower", "jungle", "Растения", "Червено цвете", 18, "/reward-assets/jungle/jungle-flower-red.svg", 0.80, 0.38, 1.70);
        addItem("jungle-purple-flower", "jungle", "Растения", "Лилаво цвете", 18, "/reward-assets/jungle/jungle-flower-purple.svg", 0.80, 0.38, 1.70);
        addItem("jungle-waterfall", "jungle", "Вода", "Водопад", 90, "/reward-assets/jungle/waterfall.svg", 1.25, 0.60, 2.55);
        addItem("jungle-rock", "jungle", "Природа", "Камък", 18, "/reward-assets/jungle/jungle-rock.svg", 0.85, 0.40, 1.80);
        addItem("jungle-log", "jungle", "Природа", "Паднало дърво", 28, "/reward-assets/jungle/jungle-log.svg", 1.00, 0.48, 2.10);
        addItem("jungle-banana-bunch", "jungle", "Плодове", "Банани", 15, "/reward-assets/jungle/banana-bunch.svg", 0.75, 0.35, 1.60);
        addItem("jungle-coconut", "jungle", "Плодове", "Кокос", 15, "/reward-assets/jungle/coconut.svg", 0.70, 0.35, 1.50);
        addItem("jungle-monkey", "jungle", "Животни", "Маймуна", 70, "/reward-assets/jungle/monkey.svg", 0.95, 0.45, 2.00);
        addItem("jungle-baby-monkey", "jungle", "Животни", "Малка маймуна", 55, "/reward-assets/jungle/baby-monkey.svg", 0.80, 0.38, 1.75);
        addItem("jungle-parrot", "jungle", "Птици", "Папагал", 55, "/reward-assets/polished/jungle/parrot.png", 0.85, 0.40, 1.85);
        addItem("jungle-toucan", "jungle", "Птици", "Тукан", 60, "/reward-assets/polished/jungle/toucan.png", 0.90, 0.42, 1.95);
        addItem("jungle-bird", "jungle", "Птици", "Пъстра птица", 45, "/reward-assets/polished/jungle/jungle-bird.png", 0.75, 0.35, 1.60);
        addItem("jungle-snake", "jungle", "Животни", "Змия", 45, "/reward-assets/jungle/snake.svg", 0.90, 0.42, 1.95);
        addItem("jungle-crocodile", "jungle", "Животни", "Крокодил", 90, "/reward-assets/polished/jungle/crocodile.png", 1.10, 0.55, 2.35);
        addItem("jungle-tiger", "jungle", "Животни", "Тигър", 100, "/reward-assets/polished/jungle/tiger.png", 1.05, 0.50, 2.25);
        addItem("jungle-tiger-cub", "jungle", "Животни", "Малко тигърче", 70, "/reward-assets/polished/jungle/tiger-cub.png", 0.82, 0.40, 1.80);
        addItem("jungle-elephant", "jungle", "Животни", "Слон", 110, "/reward-assets/polished/jungle/elephant.png", 1.15, 0.55, 2.45);
        addItem("jungle-baby-elephant", "jungle", "Животни", "Малко слонче", 75, "/reward-assets/polished/jungle/baby-elephant.png", 0.90, 0.45, 1.95);
        addItem("jungle-lion", "jungle", "Животни", "Лъв", 100, "/reward-assets/polished/jungle/lion.png", 1.05, 0.50, 2.25);
        addItem("jungle-lion-cub", "jungle", "Животни", "Малко лъвче", 70, "/reward-assets/polished/jungle/lion-cub.png", 0.82, 0.40, 1.80);
        addItem("jungle-leopard", "jungle", "Животни", "Леопард", 95, "/reward-assets/polished/jungle/leopard.png", 1.00, 0.48, 2.15);
        addItem("jungle-zebra", "jungle", "Животни", "Зебра", 80, "/reward-assets/polished/jungle/zebra.png", 1.00, 0.48, 2.15);
        addItem("jungle-giraffe", "jungle", "Животни", "Жираф", 95, "/reward-assets/polished/jungle/giraffe.png", 1.05, 0.50, 2.25);
        addItem("jungle-hippo", "jungle", "Животни", "Хипопотам", 90, "/reward-assets/polished/jungle/hippo.png", 1.05, 0.50, 2.25);
        addItem("jungle-frog", "jungle", "Животни", "Жаба", 35, "/reward-assets/polished/jungle/frog.png", 0.70, 0.35, 1.55);
        addItem("jungle-butterfly", "jungle", "Животни", "Пеперуда", 30, "/reward-assets/polished/jungle/butterfly.png", 0.65, 0.32, 1.45);
    }

    private void addTheme(String id, String name, String description, String background, String thumbnail, String defaultName) {
        themes.put(id, new RewardThemeResponse(
                id,
                name,
                description,
                background,
                thumbnail,
                List.of("Природа", "Животни", "Предмети", "Украси", "Хора", "Небе", "Храна", "Спорт", "Герои", "Пътеки", "Космос", "Планети", "Кораби", "Станции", "Галактики", "Риби", "Морски животни", "Морско дъно", "Растения", "Корали", "Съкровища", "Сгради", "Машини", "Птици", "Плодове", "Вода"),
                defaultName
        ));
    }

    private void addItem(
            String id,
            String themeId,
            String category,
            String name,
            int price,
            String image,
            double defaultScale,
            double minScale,
            double maxScale
    ) {
        items.put(id, new RewardCatalogItemResponse(id, themeId, category, name, price, image, defaultScale, minScale, maxScale));
    }
}
