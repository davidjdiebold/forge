package forge.util;

import forge.game.card.Card;
import forge.game.card.CardCollection;

import java.util.*;

public class ShuffleUtil {
    public static void shuffle(
            CardCollection list,
            int alreadyDrawn,
            Map<Integer, String> drawSchedule,
            Random random
    ) {

        try {
            if (drawSchedule != null && drawSchedule.size() != 0) {

                SortedMap<Integer, String> remainingSchedule = new TreeMap<>();
                for (Integer turn : drawSchedule.keySet()) {
                    if (turn >= alreadyDrawn) {
                        remainingSchedule.put(turn - alreadyDrawn, drawSchedule.get(turn));
                    }
                }

                if (remainingSchedule.size() > 0) {
                    List<String> toMove = new ArrayList<>(remainingSchedule.size());
                    for (Integer turn : remainingSchedule.keySet()) {
                        toMove.add(remainingSchedule.get(turn));
                    }
                    SortedMap<Integer, String> turnsWhereFound = new TreeMap<>();

                    int i = list.size() - 1;
                    while(turnsWhereFound.size()<remainingSchedule.size() && i>=0) {
                        String cardAtI = list.get(i).getName();
                        if (toMove.contains(cardAtI)) {
                            turnsWhereFound.put(i, cardAtI);
                        }
                        i--;
                    }

                    if (turnsWhereFound.size()==remainingSchedule.size()) {
                        List<Card> cards = new ArrayList<>();
                        int j = 0;
                        for (Integer turn : turnsWhereFound.keySet()) {
                            Card c = list.remove(((int)turn)-j);
                            cards.add(c);
                            ++j;
                        }

                        sort(list);
                        Collections.shuffle(list, random);
                        for (Integer turn : remainingSchedule.keySet()) {
                            Card toRemove = list.remove((int)turn);
                            Card toInsert = null;
                            i = 0;
                            while (toInsert==null) {
                                if(cards.get(i).getName().equals(remainingSchedule.get(turn))) {
                                    toInsert = cards.remove(i);
                                }
                            }
                            list.add(turn, toInsert);
                            list.add(toRemove);
                            System.out.println("Swaping " + toRemove.getName() + " with " + toInsert.getName());
                        }

                    } else {
                        System.out.println("Card in draw schedule not present in deck.");
                    }
                } else {
                    // Note: Shuffling once is sufficient.
                    sort(list);
                    Collections.shuffle(list, random);
                }
            } else {
                // Note: Shuffling once is sufficient.
                sort(list);
                Collections.shuffle(list, random);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void sort(CardCollection list) {
        List<Card> cards = new ArrayList<>(list);
        cards.sort((o1, o2) -> {
            int delta = o1.getName().compareTo(o2.getName());
            return delta != 0 ? delta : o1.getId() - o2.getId();
        });
        list.clear();
        list.addAll(cards);
    }
}
