Approach used for the transfer API:
    1. Lock would be acquired on account id's for both from and to.
    2. The lock would be acquired in order to avoid deadlock.
    3. To achieve this - account id's can be compared and lock 1 would be acquired with smaller account id.

Improvements if more time is given:

1. GetAccount API can be improved to handle invalid account id.
2. CreateAccount API can be built to generate account id automatically.

