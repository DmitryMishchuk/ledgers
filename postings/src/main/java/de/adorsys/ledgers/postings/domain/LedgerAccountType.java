package de.adorsys.ledgers.postings.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString(callSuper=true)
@NoArgsConstructor
@Table(uniqueConstraints={@UniqueConstraint(columnNames = {"name", "validFrom"}, name="LedgerAccountType_name_validFrom_unique")})
public class LedgerAccountType extends LedgerEntity {
	
	/*Containing chart of account.*/
	@Column(nullable=false)
	private String coa;

	/*For the root object, the parent carries the name of the object.*/
	@Column(nullable=false)
	private String parent;

	/*The detail level of this ledger account type*/
	private int level;
	
	@Builder
	public LedgerAccountType(String id, String name, LocalDateTime validFrom, LocalDateTime created, String user,
			String coa, String parent, int level) {
		super(id, name, validFrom, created, user);
		this.coa = coa;
		this.parent = parent;
		this.level = level;
	}
}