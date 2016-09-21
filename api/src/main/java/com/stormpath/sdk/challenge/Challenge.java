/*
* Copyright 2016 Stormpath, Inc.
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
package com.stormpath.sdk.challenge;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.factor.Factor;
import com.stormpath.sdk.factor.sms.SmsFactor;
import com.stormpath.sdk.resource.*;

/**
 * This domain object represents a challenge of a {@link Factor} for a Multi Factor Authentication.
 * <p/>
 * In a Multi Factor Authentication scenario authenticating a user is challenged by additional {@link Factor}s like an {@link SmsFactor}.
 *
 * For Example: Using an {@link SmsFactor} as an additional {@link Factor} for authentication the user would receive an sms including a multi-digit code within its message.
 * The user would verify the authentication challenge by entering the sms code back to the system.
 *
 * @since 1.1.0
 */
public interface Challenge extends Resource, Saveable, Deletable, Auditable {

    /**
     * Returns the message associated with this challenge.
     * The message contains a code sent to the user to be sent back
     * for authentication.
     *
     * @return message associated with this challenge
     */
    String getMessage();

    /**
     * Sets the message associated with this challenge.
     * Tenant.
     *
     * @param message the message associated with this challenge.
     * @return this instance for method chaining.
     */
    Challenge setMessage(String message);

    /**
     * Returns the status of this challenge object
     *
     * @return status associated with this challenge
     */
    ChallengeStatus getStatus();

    /**
     * Sets the status associated with this challenge.
     * Tenant.
     *
     * @param status the status associated with this challenge.
     * @return this instance for method chaining.
     */
    Challenge setStatus(ChallengeStatus status);

    /**
     * Returns the account associated with this challenge
     *
     * @return account associated with this challenge
     */
    Account getAccount();

    /**
     * Sets the account associated with this challenge.
     *
     * @param account associated with this challenge
     */
    Challenge setAccount(Account account);

    /**
     * Returns the sms factor associated with this challenge
     *
     * @return sms factor associated with this challenge
     */
    Factor getFactor();

    /**
     * Sets the factor associated with this challenge.
     *
     * @param smsFactor associated with this challenge
     */
    Challenge setFactor(Factor smsFactor);

    /**
     * Returns the sent code in the sms associated with this challenge
     *
     * @return code in the sms associated with this challenge
     */
    Challenge setCode(String code);

    /**
     * Returns true in case the challenge is validated with the given code
     * and false if otherwise
     *
     * @return true in case the challenge is validated with the given code
     */
    boolean validate(String code);
}
