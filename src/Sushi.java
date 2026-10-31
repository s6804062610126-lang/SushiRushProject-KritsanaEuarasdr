public abstract class Sushi {
    public static final int NONE = -1;
    public static final int SALMON = 0;
    public static final int TUNA = 1;
    public static final int TAMAGO = 2;
    public static final int MAKI = 3;

    public final int type;

    protected Sushi(int type) {
        this.type = type;
    }

    // ซูชิแต่ละชนิดบอกชื่อตัวเอง (polymorphism)
    public abstract String getName();

    public static Sushi fromIngredients(boolean hasRice, boolean hasSeaweed,
            boolean hasSalmon, boolean hasTuna, boolean hasTamago) {
        boolean hasTopping = hasSalmon || hasTuna || hasTamago;
        if (!hasRice || !hasTopping) {
            return null;
        }
        if (hasSeaweed) {
            return new Maki();
        }
        if (hasSalmon) {
            return new SalmonNigiri();
        }
        if (hasTuna) {
            return new TunaNigiri();
        }
        return new TamagoNigiri();
    }

    public static int typeFromIngredients(boolean hasRice, boolean hasSeaweed,
            boolean hasSalmon, boolean hasTuna, boolean hasTamago) {
        Sushi sushi = fromIngredients(hasRice, hasSeaweed, hasSalmon, hasTuna, hasTamago);
        return sushi == null ? NONE : sushi.type;
    }
}

class SalmonNigiri extends Sushi {
    SalmonNigiri() {
        super(SALMON);
    }

    @Override
    public String getName() {
        return "SALMON NIGIRI";
    }
}

class TunaNigiri extends Sushi {
    TunaNigiri() {
        super(TUNA);
    }

    @Override
    public String getName() {
        return "TUNA NIGIRI";
    }
}

class TamagoNigiri extends Sushi {
    TamagoNigiri() {
        super(TAMAGO);
    }

    @Override
    public String getName() {
        return "TAMAGO NIGIRI";
    }
}

class Maki extends Sushi {
    Maki() {
        super(MAKI);
    }

    @Override
    public String getName() {
        return "MAKI ROLL";
    }
}
