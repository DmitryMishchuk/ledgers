package de.adorsys.ledgers.postings.db.adapter;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.adorsys.ledgers.postings.db.domain.FinancialStmt;
import de.adorsys.ledgers.postings.db.domain.Ledger;
import de.adorsys.ledgers.postings.db.domain.LedgerAccount;
import de.adorsys.ledgers.postings.db.domain.Posting;
import de.adorsys.ledgers.postings.db.domain.PostingLine;
import de.adorsys.ledgers.postings.db.domain.StmtStatus;
import de.adorsys.ledgers.postings.db.domain.StmtType;
import de.adorsys.ledgers.postings.db.exception.BaseLineException;
import de.adorsys.ledgers.postings.db.exception.DoubleEntryAccountingException;
import de.adorsys.ledgers.postings.db.exception.PostingRepositoryException;
import de.adorsys.ledgers.postings.db.repository.FinancialStmtRepository;
import de.adorsys.ledgers.postings.db.repository.LedgerAccountRepository;
import de.adorsys.ledgers.postings.db.repository.PostingLineRepository;
import de.adorsys.ledgers.postings.db.repository.PostingRepository;
import de.adorsys.ledgers.util.CloneUtils;

@Service
public class PostingRepositoryAdapter {

	@Autowired
	private LedgerAccountRepository ledgerAccountRepository;
	@Autowired
	private PostingRepository postingRepository;
	@Autowired
	private PostingLineRepository postingLineRepository;
	@Autowired
	private FinancialStmtRepository financialStmtRepository;
	@Autowired
	private Principal principal;
	
	public PostingLine computeBalance(LedgerAccount ledgerAccount, LocalDateTime refTime) {
		Ledger ledger = ledgerAccount.getLedger();
		// Load last closed balance statement for this account.
		// Load corresponding posting
		// This posting will generaly contain a single line.
		String stmtTarget = ledgerAccount.getId();
		Posting posting = financialStmtRepository
				.findFirstByLedgerAndStmtTypeAndStmtTargetAndStmtStatusOrderByPstTimeDesc(ledger, StmtType.ACC,
						stmtTarget, StmtStatus.CLOSED)
				.map(stmt -> stmt.getPosting()).orElse(null);
		PostingLine baseLine = posting == null ? null
				: posting.getLines().stream().filter(pl -> pl.getAccount().getName().equals(ledgerAccount.getName()))
						.findFirst().orElseThrow(() -> new IllegalStateException());

		String baseLineId = baseLine == null ? null : baseLine.getId();
		// Posting posting = lastBalanceStmt.getPosting();

		// Load all posting associated with this base line.
		List<PostingLine> postingLines = baseLine == null
				? postingLineRepository.findByAccountAndPstTimeLessThanEqualOrderByRecordTimeDesc(ledgerAccount,
						refTime)
				: postingLineRepository.findByBaseLineAndPstTimeLessThanEqualOrderByRecordTimeDesc(baseLineId, refTime);

		return computeBalance(baseLine, postingLines, ledgerAccount);
	}

	public Posting newPosting(Ledger ledger, Posting posting) throws DoubleEntryAccountingException, BaseLineException{
		// check posting time is not before a closing.
		validatePostingTime(ledger, posting);

		// Load original posting. If the sequence number > 0, it means that there is a
		// predecessor posting available.
		loadPredecessor(posting).ifPresent(posting::union);

		// Check the ledger
		validateDoubleEntryAccounting(posting);

		// find last record.
		Posting antecedent = postingRepository.findFirstOptionalByLedgerOrderByRecordTimeDesc(posting.getLedger())
				.orElse(new Posting());

		List<PostingLine> postingLines = CloneUtils.cloneList(posting.getLines(), PostingLine.class);
		Posting p = new Posting(principal.getName(), antecedent.getId(), antecedent.getHash(), posting.getOprId(),
				posting.getOprSeqNbr(), posting.getOprTime(), posting.getOprType(), posting.getOprDetails(),
				posting.getPstTime(), posting.getPstType(), posting.getPstStatus(), ledger, posting.getValTime(), null);

		Posting saved = postingRepository.save(p);
		String postingId = saved.getId();
		saved = postingRepository.findById(postingId).orElseThrow(() -> new IllegalStateException(postingId));

		saved = postingRepository.save(saved.hash());

		// Validate existence of accounts and make sure they are all in the same ledger.
		for (PostingLine line : postingLines) {
			LedgerAccount ledgerAccount = ledgerAccountRepository
					.findOptionalByLedgerAndName(ledger, line.getAccount().getName())
					.orElseThrow(() -> insufficientInfo(line.getAccount()));
			String baseLine = validatePostingTime(ledger, posting, ledgerAccount).map(b -> b.getId()).orElse(null);

			PostingLine pl = new PostingLine(null, saved, ledgerAccount, line.getDebitAmount(), line.getCreditAmount(),
					line.getDetails(), line.getSrcAccount(), baseLine);
			postingLineRepository.save(pl);
		}

		return saved;

	}

	/**
	 * Validate Double Entry Accounting. Make sure both total of debit an credit of the 
	 * given posting lines are equal.
	 * 
	 * @param posting
	 * @throws DoubleEntryAccountingException
	 */
	private void validateDoubleEntryAccounting(Posting posting) throws DoubleEntryAccountingException {
		List<PostingLine> lines = posting.getLines();
		BigDecimal sumDebit = BigDecimal.ZERO;
		BigDecimal sumCredit = BigDecimal.ZERO;
		for (PostingLine line : lines) {
			sumDebit = sumDebit.add(line.getDebitAmount());
			sumCredit = sumCredit.add(line.getCreditAmount());
		}

		if (!sumDebit.equals(sumCredit)) {
			throw new DoubleEntryAccountingException(sumDebit, sumCredit);
		}
	}
	
	/**
	 * Check if there is any closed financial statement after the posting time.
	 * Throws a baseline exception if this is the case.
	 * 
	 * @param ledger
	 * @param posting
	 * @throws BaseLineException
	 * @throws PostingRepositoryException
	 */
	private void validatePostingTime(Ledger ledger, Posting posting)
			throws BaseLineException, PostingRepositoryException {
		// check posting time not null
		postingTimeNotNull(posting);

		// Look for an account statement closed before that date.
		Optional<FinancialStmt> stmtOpt = financialStmtRepository
				.findFirstByLedgerAndStmtTypeAndStmtTargetAndStmtStatusOrderByPstTimeDesc(
						ledger, StmtType.BS, ledger.getId(), StmtStatus.CLOSED);

		if (stmtOpt.isPresent()) {
			FinancialStmt accStmt = stmtOpt.get();
			if (posting.getPstTime().isBefore(accStmt.getPstTime())) {
				throw new BaseLineException(posting.getPstTime(), accStmt.getPstTime());
			}
		}
	}
	
	/**
	 * Checks if this account has a released financial statement by the given posting date.
	 * 
	 * @param ledger
	 * @param posting
	 * @param ledgerAccount
	 * @return
	 * @throws BaseLineException
	 * @throws PostingRepositoryException 
	 */
	private Optional<PostingLine> validatePostingTime(Ledger ledger, Posting posting, LedgerAccount ledgerAccount) throws BaseLineException, PostingRepositoryException {
		// check posting time not null
		postingTimeNotNull(posting);

		// Look for an account statement closed before that date.
		Optional<FinancialStmt> stmtOpt = financialStmtRepository
				.findFirstByLedgerAndStmtTypeAndStmtTargetAndStmtStatusOrderByPstTimeDesc(
						ledger, StmtType.ACC, ledgerAccount.getId(), StmtStatus.CLOSED);
		
		if (!stmtOpt.isPresent()) {
			return Optional.empty();
		}
		
		FinancialStmt accStmt = stmtOpt.get();
		if (posting.getPstTime().isAfter(accStmt.getPstTime())) {
			return accStmt.getPosting().getLines().stream()
					.filter(pl -> pl.getAccount().getName().equals(ledgerAccount.getName())).findFirst();
		}
		throw new BaseLineException(posting.getPstTime(), accStmt.getPstTime());
	}	
	
	private void postingTimeNotNull(Posting posting) throws PostingRepositoryException {
		if (posting.getPstTime() == null) {
			throw new PostingRepositoryException("Missing posting time");
		}
	}

	private PostingLine computeBalance(PostingLine baseLine, List<PostingLine> lines, LedgerAccount account) {
		BigDecimal sumDebit = baseLine == null ? BigDecimal.ZERO : baseLine.getDebitAmount();
		BigDecimal sumCredit = baseLine == null ? BigDecimal.ZERO : baseLine.getCreditAmount();
		Set<String> discardedPostings = filterPostings(lines);
		for (PostingLine line : lines) {
			if (discardedPostings.contains(line.getPosting().getId())) {
				continue;
			}
			sumDebit = sumDebit.add(line.getDebitAmount());
			sumCredit = sumCredit.add(line.getCreditAmount());
		}
		return new PostingLine(null, null, account, sumDebit, sumCredit, null, null, null);
	}

	private Set<String> filterPostings(List<PostingLine> lines) {
		// building computation set:
		Set<String> discardedPostings = new HashSet<>();
		for (PostingLine line : lines) {
			if (line.getOprSeqNbr() > 0) {
				for (int i = line.getOprSeqNbr() - 1; i >= 0; i--) {
					String makeId = Posting.makeId(line.getOprId(), i);
					discardedPostings.add(makeId);
				}
			}
		}
		return discardedPostings;
	}

	// TODO consider creating of exception builder with all necessary classes
	protected IllegalArgumentException insufficientInfo(Object modelObject) {
		return new IllegalArgumentException(
				String.format("Model Object does not provide sufficient information for loading original instance. %s",
						modelObject.toString()));
	}

	private Optional<Posting> loadPredecessor(Posting current) {
		return current.clonedId().map(postingRepository::findById).orElse(Optional.empty());
	}

	public Optional<LedgerAccount> loadLedgerAccount(Ledger ledger, String accountNumber) {
		return ledgerAccountRepository.findOptionalByLedgerAndName(ledger, accountNumber);
	}

	public void createLedgerAccount(LedgerAccount la) {
		ledgerAccountRepository.save(la);
	}

}