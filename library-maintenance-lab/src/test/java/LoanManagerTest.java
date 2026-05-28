import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LoanManagerTest {

    // TESTE 1: Validar título nulo do livro
    @Test
    void shouldThrowExceptionWhenBookTitleIsNull() {
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                LegacyDatabase.addBookData(null, "Autor", 2020, "Categoria", 1, 1, "A1", "ISBN-123");
            }
        );
    }

    // TESTE 2: Validar atualização de usuário inexistente
    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                LegacyDatabase.unsafeUpdateUserField(999, "name", "Marcela Silva");
            }
        );
    }

    // TESTE 3: Validar empréstimo com usuário inválido/inexistente
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

    // TESTE 4: Garantir que o método refatorado lança exceção para parâmetros inválidos gerais
    @Test
    void shouldValidateLoanParametersCorrectly() {
        LoanManager loanManager = new LoanManager();
        
        // Testando com ID de livro inválido (-1) para acionar a validação limpa que refatoramos
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                loanManager.borrowBook(1, -1, "2026-05-28", "2026-06-11", "email", 14, "test", 0);
            }
        );
    }
}