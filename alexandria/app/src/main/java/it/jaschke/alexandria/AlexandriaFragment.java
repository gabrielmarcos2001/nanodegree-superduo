package it.jaschke.alexandria;

import it.jaschke.alexandria.data.Book;

/**
 * Created by gabrielmarcos on 8/31/15.
 */
public interface AlexandriaFragment {

    /**
     * Displays a Message in the Fragment - super useful
     * for displaying errors
     * @param message
     */
    void showMessage(String message);

    /**
     * A New book was fetched
     * @param book
     */
    void newBookDataFetched(Book book);
}
