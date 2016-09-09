package com.stormpath.sdk.impl.challenge

import com.stormpath.sdk.account.Account
import com.stormpath.sdk.challenge.Challenge
import com.stormpath.sdk.client.ClientIT
import com.stormpath.sdk.directory.Directory
import com.stormpath.sdk.factor.sms.SmsFactor
import com.stormpath.sdk.phone.Phone
import com.stormpath.sdk.resource.ResourceException
import org.testng.annotations.Test
import static org.testng.AssertJUnit.assertEquals
import static org.testng.AssertJUnit.assertTrue

class ChallengeIT extends ClientIT{

    @Test
    void testFailedChallenge() {
        Directory dir = client.instantiate(Directory)
        dir.name = uniquify("Java SDK: DirectoryIT.testCreateAndDeleteDirectory")
        dir = client.currentTenant.createDirectory(dir);
        Account account = client.instantiate(Account)
        account = account.setGivenName('John')
                .setSurname('DELETEME')
                .setEmail('johndeleteme@nowhere.com')
                .setPassword('Changeme1!')

        deleteOnTeardown(account)
        deleteOnTeardown(dir)

        dir.createAccount(account)

        def phone = client.instantiate(Phone)
        phone = phone.setNumber("6123438710").setAccount(account)
        SmsFactor factor = client.instantiate(SmsFactor)
        factor = factor.setPhone(phone)

        factor = account.createFactor(factor)

        def challenge = client.instantiate(Challenge)
        challenge.setMessage("This the message which has no place holder for the code")

        // A 13103 is returned sice message does not contain a ${code}
        try {
            factor.createChallenge(challenge)
        }
        catch(ResourceException re){
            assertEquals(re.status, 400)
            assertEquals(re.getCode(), 13103)
        }
    }

    @Test
    void testSuccessfulChallengeSendCode() {
        Directory dir = client.instantiate(Directory)
        dir.name = uniquify("Java SDK: DirectoryIT.testCreateAndDeleteDirectory")
        dir = client.currentTenant.createDirectory(dir);
        Account account = client.instantiate(Account)
        account = account.setGivenName('John')
                .setSurname('DELETEME')
                .setEmail('johndeleteme@nowhere.com')
                .setPassword('Changeme1!')

        deleteOnTeardown(account)
        deleteOnTeardown(dir)

        dir.createAccount(account)

        def phone = client.instantiate(Phone)
        phone = phone.setNumber("2016589573").setAccount(account)
        SmsFactor factor = client.instantiate(SmsFactor)
        factor = factor.setPhone(phone)

        factor = account.createFactor(factor)

        def challenge = client.instantiate(Challenge)
        challenge.setMessage("This is your owesome code: \${code}")

        challenge = factor.createChallenge(challenge)

        println("Chalenge href: $challenge.href")

        assertEquals(challenge.href.substring(challenge.href.lastIndexOf("/")+1), challenge.token)
    }

    @Test
    void testSuccessfulChallengeVerifyChallenge() {

        def href = "http://localhost:9191/v1/challenges/3YA1MvXeccKORSkX6QXkB4"
        def code = "506726"
        Challenge challenge = client.getResource(href, Challenge)
        assertTrue(challenge.validate(code))

    }
}
