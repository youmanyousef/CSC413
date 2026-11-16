## Milestone 7
### Joseph Altareb

1. Group

I worked alone
2. Tools

- IntelliJ IDEA with the Antigravity Plugin
- Gemini 3.8 Flash
3. Other view

`ColorView` - Terminal based view with ANSI color codes.
```bash
java -cp target/classes edu.sfsu.csc413.chess.Main color
```
4. Runs

<details>
<summary>Run 1</summary>

```m7-run-1```

Prompt: Add a graphical 2D view of the board using Swing. I want to play by clicking a piece and then clicking the square to move it to. Show whose turn it is and messages like check or illegal moves. Follow AGENTS.md.

</details>

<details>
<summary>Run 2</summary>

```m7-run-2```

Prompt: Add a graphical 2D view of the board using Swing. I want to play by clicking a piece and then clicking the square to move it to. Show whose turn it is in a convenient spot. There should be alert messages like when the king is in check. Try displaying smoothing animations when pieces are moved. Follow AGENTS.md.

What changed? The layout was inconvenient and there were no animations, so I prompted AI to add them. 
</details>
<details>
<summary>Run 3</summary>

```m7-run-3```

Prompt: Add a graphical 2D view of the board using Swing. I want to play by clicking a piece and then clicking the square to move it to. Show whose turn it is in a convenient spot. There should be alert messages like when the king is in check. Promotion choices will spawn a new window, prompting the user to choose a PieceType. Illegal moves should not spawn the promotion choices window. Try displaying smoothing animations when pieces are moved. Follow AGENTS.md.

What changed? I wanted to emphasize alerts and also stop the view from opening the promotion choices panel every turn.
</details>

<details>
<summary>Run 4</summary>

```m7-run-4```

Prompt: Add a terminal view of the board and call it `ColorView`. The gameplay is exactly like ConsoleView and EmojiView. The pieces are distinguished by their piece symbol, and the background for that character is matched with the side it's from. For example, a black pawn would have a black background cell, a white 'p' character for contrast. Alerts like when the king is check should have a red background, announcing the winner should have a green background, and everything else has a gray background. When the king is in check, the king's background becomes red as well. Empty spaces on the board should have a light brown color. Clear the screen after every step in the game loop. Follow AGENTS.md.
</details>

5. `AGENTS.md`

I didn't change much from this file except adding a line that forced the AI to disclose what files it had created.

6. Review 

...

7. Comparison

...

8,9,10,11,12
...

13. Debugger evidence

`SwingView` had a bug where the _CHECK_ status kept writing over any messages coming in from the controller. For example: if you wanted to list the legal moves left when in check, it would get printed and instantly get overwritten by "King is in check!."

I started by putting a breakpoint in `alertLabel.setText("⚠️ CHECK! " + side.name() + " King is in check!");`, `SwingView`, around line 386. I also put a breakpoint in `alertLabel.setText(message);` in `showMessage()`, around line 285. I played a variation of fools mate that let the player have a few moves left. When I got there, stepping through the loop, the message was coming through but immediately got overwritten. 

`alertLabel` gets set to the message "2 legal moves: e1e2 g2g3 ", after stepping out of this first breakpoint, the second breakpoint hits. `alertLabel` gets set to the _CHECK_ message because _CHECK_ doesn't have anything blocking it from showing. As long as the `GameStatus` is _CHECK_, the message will show, and it is last thing changing `alertLabel`

14. By hand

I fixed this bug by adding a new variable to track whether the panel already showed the _CHECK_ warning. `currentlyInCheck` is set to _false_ by default, but then it's set to _true_ when _CHECK_ is first caught. After that, the panel's background doesn't change, but the message from the controller finally comes through since it's not being overwritten.
