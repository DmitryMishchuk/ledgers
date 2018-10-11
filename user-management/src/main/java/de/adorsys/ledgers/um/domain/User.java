/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.ledgers.um.domain;

import de.adorsys.ledgers.postings.domain.LedgerAccount;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = "login", name = "user_login_unique"),
        @UniqueConstraint(columnNames = "email", name = "user_email_unique")
})
public class User {
    @Id
    @Column(name = "user_id")
    private String id;

    @NotNull
    private String login;

    @NotNull
    private String email;

    @NotNull
    private String pin;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_ledger_account",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "ledger_account_id", referencedColumnName = "id")
    )
    private List<LedgerAccount> accounts;
}