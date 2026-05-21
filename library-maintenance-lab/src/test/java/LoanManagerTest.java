import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LoanManagerTest {

    // TESTE 1: Validar título nulo do livro (Já corrigido!)
    @Test
    void shouldThrowExceptionWhenBookTitleIsNull() {
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                LegacyDatabase.addBookData(null, "Autor", 2020, "Categoria", 1, 1, "A1", "ISBN-123");
            }
        );
    }

    // TESTE 2: Validar atualização de usuário inexistente (Já corrigido!)
    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                LegacyDatabase.unsafeUpdateUserField(999, "name", "Marcela Silva");
            }
        );
    }

    // TESTE 3: Validar empréstimo com dados inválidos (O novo Bug!)
    @Test
    void shouldThrowIllegalArgumentExceptionWhenUserOrBookIsInvalid() {
        LoanManager loanManager = new LoanManager();
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                loanManager.borrowBook(999, 1, "2026-05-21", "2026-06-04", "email", 14, "test", 0);
            }
        );
    }
}