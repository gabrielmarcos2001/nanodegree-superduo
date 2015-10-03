package it.jaschke.alexandria;

import it.jaschke.alexandria.data.Book;

/**
 * Created by gabrielmarcos on 8/31/15.
 */
public interface AlexandriaFragment {

    void showMessage(String message);
    void newBookDataFetched(Book book);
}
