import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
    Avatar,
    Box,
    Button,
    Chip,
    CircularProgress,
    Container,
    Divider,
    Paper,
    Stack,
    TextField,
    Typography
} from "@mui/material";
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ForumOutlinedIcon from '@mui/icons-material/ForumOutlined';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import ReplyOutlinedIcon from '@mui/icons-material/ReplyOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import StarRoundedIcon from '@mui/icons-material/StarRounded';
import {useParams} from "react-router-dom";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {GameComment} from "../GeneralComponents/types/GameComment";
import {GameData} from "../GeneralComponents/types/GameData";
import {formatDisplayDate} from "../GeneralComponents/utils/dates";
import {formatTenPointRating, getRatingVisuals, parseRatingValue} from "../GeneralComponents/utils/ratings";
import ListModificationModal from "./ListModificationModal";
import {getErrorMessage} from "../api/client";
import {
    createGameComment,
    deleteGameComment,
    getGame,
    getGameComments,
    recordGameInteraction,
    removeWishlistEntry,
    updateGameComment,
    upsertWishlist
} from "../api/games";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {requiresPlatformSelection} from "../GeneralComponents/utils/platforms";

const COMMENTS_PAGE_SIZE = 12;

function buildCollapsedReplyState(thread: GameComment[], previousState: Record<string, boolean> = {}): Record<string, boolean> {
    return thread.reduce<Record<string, boolean>>((accumulator, comment) => {
        if (comment.replies.length > 0) {
            accumulator[comment.id] = previousState[comment.id] ?? true;
        }

        return {
            ...accumulator,
            ...buildCollapsedReplyState(comment.replies, previousState)
        };
    }, {});
}

function insertCommentIntoThread(thread: GameComment[], createdComment: GameComment): GameComment[] {
    if (!createdComment.parentCommentId) {
        return [createdComment, ...thread];
    }

    let inserted = false;
    const nextThread = thread.map(comment => {
        const [updatedComment, wasInserted] = insertReply(comment, createdComment);
        if (wasInserted) {
            inserted = true;
        }
        return updatedComment;
    });

    return inserted ? nextThread : [createdComment, ...thread];
}

function insertReply(comment: GameComment, createdComment: GameComment): [GameComment, boolean] {
    if (comment.id === createdComment.parentCommentId) {
        return [{...comment, replies: [...comment.replies, createdComment]}, true];
    }

    let inserted = false;
    const nextReplies = comment.replies.map(reply => {
        const [updatedReply, wasInserted] = insertReply(reply, createdComment);
        if (wasInserted) {
            inserted = true;
        }
        return updatedReply;
    });

    if (!inserted) {
        return [comment, false];
    }

    return [{...comment, replies: nextReplies}, true];
}

function replaceCommentInThread(thread: GameComment[], updatedComment: GameComment): GameComment[] {
    let changed = false;
    const nextThread = thread.map(comment => {
        if (comment.id === updatedComment.id) {
            changed = true;
            return {...comment, ...updatedComment, replies: comment.replies};
        }
        if (comment.replies.length === 0) {
            return comment;
        }

        const nextReplies = replaceCommentInThread(comment.replies, updatedComment);
        if (nextReplies !== comment.replies) {
            changed = true;
            return {...comment, replies: nextReplies};
        }
        return comment;
    });

    return changed ? nextThread : thread;
}

function deleteCommentFromThread(thread: GameComment[],
                                 commentId: string,
                                 depth = 0): { nextThread: GameComment[]; removedTopLevel: boolean; changed: boolean } {
    let changed = false;
    let removedTopLevel = false;
    const nextThread: GameComment[] = [];

    thread.forEach(comment => {
        if (comment.id === commentId) {
            changed = true;
            if (comment.replies.length > 0) {
                nextThread.push({
                    ...comment,
                    content: '',
                    deleted: true
                });
            } else if (depth === 0) {
                removedTopLevel = true;
            }
            return;
        }

        const nestedResult = deleteCommentFromThread(comment.replies, commentId, depth + 1);
        if (nestedResult.changed) {
            changed = true;
            removedTopLevel = removedTopLevel || nestedResult.removedTopLevel;
            nextThread.push({...comment, replies: nestedResult.nextThread});
            return;
        }

        nextThread.push(comment);
    });

    return {
        nextThread: changed ? nextThread : thread,
        removedTopLevel,
        changed
    };
}

function GameMainPage() {
    const {gameId = ''} = useParams<string>();
    const [game, setGame] = useState<GameData | null>(null);
    const [comments, setComments] = useState<GameComment[]>([]);
    const [commentDraft, setCommentDraft] = useState('');
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const [modalOpen, setModalOpen] = useState(false);
    const [commentsLoading, setCommentsLoading] = useState(true);
    const [commentsHasMore, setCommentsHasMore] = useState(false);
    const [loadingMoreComments, setLoadingMoreComments] = useState(false);
    const [postingTargetId, setPostingTargetId] = useState<string | null>(null);
    const [activeReplyId, setActiveReplyId] = useState<string | null>(null);
    const [replyDrafts, setReplyDrafts] = useState<Record<string, string>>({});
    const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
    const [editDrafts, setEditDrafts] = useState<Record<string, string>>({});
    const [updatingCommentId, setUpdatingCommentId] = useState<string | null>(null);
    const [deletingCommentId, setDeletingCommentId] = useState<string | null>(null);
    const [collapsedReplyIds, setCollapsedReplyIds] = useState<Record<string, boolean>>({});
    const commentsSentinelRef = useRef<HTMLDivElement | null>(null);
    const commentsRequestIdRef = useRef(0);

    useEffect(() => {
        let isMounted = true;
        const nextRequestId = commentsRequestIdRef.current + 1;
        commentsRequestIdRef.current = nextRequestId;
        setGame(null);
        setComments([]);
        setCommentsHasMore(false);
        setCollapsedReplyIds({});
        setCommentsLoading(true);
        setLoadingMoreComments(false);

        Promise.all([getGame(gameId), getGameComments(gameId, COMMENTS_PAGE_SIZE, 0)])
            .then(([gameResponse, commentsResponse]) => {
                if (!isMounted || commentsRequestIdRef.current !== nextRequestId) {
                    return;
                }
                setGame(gameResponse);
                setComments(commentsResponse.items);
                setCommentsHasMore(commentsResponse.hasMore);
                setCollapsedReplyIds(previous => buildCollapsedReplyState(commentsResponse.items, previous));
            })
            .catch(error => {
                if (isMounted && commentsRequestIdRef.current === nextRequestId) {
                    setMessage(getErrorMessage(error));
                    setSeverity(Severity.ERROR);
                }
            })
            .finally(() => {
                if (isMounted && commentsRequestIdRef.current === nextRequestId) {
                    setCommentsLoading(false);
                }
            });

        recordGameInteraction(gameId).catch(() => null);

        return () => {
            isMounted = false;
        };
    }, [gameId]);

    const loadMoreComments = useCallback(async () => {
        if (commentsLoading || loadingMoreComments || !commentsHasMore) {
            return;
        }

        const nextRequestId = commentsRequestIdRef.current + 1;
        commentsRequestIdRef.current = nextRequestId;
        setLoadingMoreComments(true);

        try {
            const response = await getGameComments(gameId, COMMENTS_PAGE_SIZE, comments.length);
            if (commentsRequestIdRef.current !== nextRequestId) {
                return;
            }

            setComments(previous => {
                const nextComments = [...previous, ...response.items];
                setCollapsedReplyIds(current => buildCollapsedReplyState(nextComments, current));
                return nextComments;
            });
            setCommentsHasMore(response.hasMore);
        } catch (error) {
            if (commentsRequestIdRef.current === nextRequestId) {
                setMessage(getErrorMessage(error));
                setSeverity(Severity.ERROR);
            }
        } finally {
            if (commentsRequestIdRef.current === nextRequestId) {
                setLoadingMoreComments(false);
            }
        }
    }, [comments.length, commentsHasMore, commentsLoading, gameId, loadingMoreComments]);

    useEffect(() => {
        if (!commentsSentinelRef.current || commentsLoading || loadingMoreComments || !commentsHasMore) {
            return;
        }

        const observer = new IntersectionObserver(entries => {
            if (!entries[0]?.isIntersecting) {
                return;
            }
            loadMoreComments().catch(() => null);
        }, {rootMargin: '260px 0px'});

        observer.observe(commentsSentinelRef.current);
        return () => observer.disconnect();
    }, [commentsHasMore, commentsLoading, loadMoreComments, loadingMoreComments]);

    async function handleQuickAdd() {
        if (!game) {
            return;
        }

        if (requiresPlatformSelection(game.platforms)) {
            setModalOpen(true);
            setMessage('Choose a platform for this game before adding it to your list.');
            setSeverity(Severity.INFO);
            return;
        }

        try {
            await upsertWishlist(game.id, GameStatusTypes.PLAN_TO_PLAY, null);
            setGame({...game, inWishlist: true});
            setMessage(`${game.name} added to your list.`);
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    async function handleRemove() {
        if (!game) {
            return;
        }

        try {
            await removeWishlistEntry(game.id);
            setGame({...game, inWishlist: false});
            setMessage(`${game.name} removed from your list.`);
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    async function submitComment(content: string, parentCommentId?: string | null) {
        if (!game || !content.trim()) {
            return;
        }

        try {
            setPostingTargetId(parentCommentId || 'root');
            const createdComment = await createGameComment(game.id, content.trim(), parentCommentId);
            setComments(previous => {
                const nextComments = insertCommentIntoThread(previous, createdComment);
                setCollapsedReplyIds(current => ({
                    ...buildCollapsedReplyState(nextComments, current),
                    ...(parentCommentId ? {[parentCommentId]: false} : {})
                }));
                return nextComments;
            });
            setGame(previous => previous ? {
                ...previous,
                comments: String(Number(previous.comments || '0') + 1)
            } : previous);
            if (parentCommentId) {
                setReplyDrafts(previous => ({...previous, [parentCommentId]: ''}));
                setActiveReplyId(current => current === parentCommentId ? null : current);
                setMessage('Reply posted.');
            } else {
                setCommentDraft('');
                setMessage('Comment posted.');
            }
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setPostingTargetId(null);
        }
    }

    async function handleCommentSubmit() {
        await submitComment(commentDraft);
    }

    async function handleReplySubmit(parentCommentId: string) {
        await submitComment(replyDrafts[parentCommentId] || '', parentCommentId);
    }

    async function handleEditSubmit(commentId: string) {
        if (!game || !editDrafts[commentId]?.trim()) {
            return;
        }

        try {
            setUpdatingCommentId(commentId);
            const updatedComment = await updateGameComment(game.id, commentId, editDrafts[commentId].trim());
            setComments(previous => replaceCommentInThread(previous, updatedComment));
            setEditingCommentId(null);
            setMessage('Comment updated.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setUpdatingCommentId(null);
        }
    }

    async function handleDeleteComment(commentId: string) {
        if (!game) {
            return;
        }

        try {
            setDeletingCommentId(commentId);
            await deleteGameComment(game.id, commentId);
            setComments(previous => {
                const deletionResult = deleteCommentFromThread(previous, commentId);
                setCollapsedReplyIds(current => buildCollapsedReplyState(deletionResult.nextThread, current));
                return deletionResult.nextThread;
            });
            setEditingCommentId(current => current === commentId ? null : current);
            setActiveReplyId(current => current === commentId ? null : current);
            setGame(previous => previous ? {
                ...previous,
                comments: String(Math.max(0, Number(previous.comments || '0') - 1))
            } : previous);
            setMessage('Comment removed.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setDeletingCommentId(null);
        }
    }

    function formatDateTime(dateValue?: string | null) {
        if (!dateValue) {
            return 'Unknown';
        }

        const date = new Date(dateValue);
        if (Number.isNaN(date.getTime())) {
            return dateValue;
        }

        return date.toLocaleString();
    }

    function formatReleaseDate(dateValue?: string | null) {
        return formatDisplayDate(dateValue, {month: 'long', day: 'numeric', year: 'numeric'}, 'Release date unknown');
    }

    function renderComment(comment: GameComment, depth = 0): React.ReactNode {
        const replyDraft = replyDrafts[comment.id] || '';
        const replyOpen = activeReplyId === comment.id;
        const editOpen = editingCommentId === comment.id;
        const editDraft = editDrafts[comment.id] ?? comment.content;
        const isReplySubmitting = postingTargetId === comment.id;
        const isEditingComment = updatingCommentId === comment.id;
        const isDeletingComment = deletingCommentId === comment.id;
        const repliesCollapsed = !!collapsedReplyIds[comment.id];
        const replyIndent = `${Math.min(depth + 1, 3) * 1.5}rem`;

        return (
            <Stack key={comment.id} spacing={1.5}>
                <Paper
                    sx={{
                        p: 2.5,
                        borderRadius: '1.25rem',
                        backgroundColor: 'rgba(250,248,255,0.84)',
                        border: '1px solid rgba(69, 45, 128, 0.08)'
                    }}
                >
                    <Stack direction="row" spacing={2} alignItems="flex-start">
                        <Avatar src={comment.authorAvatarUrl || undefined}>
                            {comment.authorDisplayName.slice(0, 1).toUpperCase()}
                        </Avatar>
                        <Box sx={{flex: 1}}>
                            <Stack direction={{xs: 'column', sm: 'row'}} justifyContent="space-between" spacing={1}>
                                <Box>
                                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                                        <Typography fontWeight={800}>{comment.authorDisplayName}</Typography>
                                        {comment.authorAdmin && (
                                            <Chip
                                                label="Admin"
                                                size="small"
                                                color="secondary"
                                                sx={{fontWeight: 800}}
                                            />
                                        )}
                                    </Stack>
                                    <Typography variant="body2" color="text.secondary">
                                        @{comment.authorUsername}
                                    </Typography>
                                </Box>
                                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                                    <Typography variant="body2" color="text.secondary">
                                        {formatDateTime(comment.createdAt)}
                                    </Typography>
                                    {comment.edited && !comment.deleted && (
                                        <Chip label="Edited" size="small" variant="outlined"/>
                                    )}
                                </Stack>
                            </Stack>
                            <Typography
                                sx={{
                                    mt: 1.2,
                                    whiteSpace: 'pre-wrap',
                                    color: comment.deleted ? 'text.secondary' : 'text.primary',
                                    fontStyle: comment.deleted ? 'italic' : 'normal'
                                }}
                            >
                                {comment.deleted ? 'Comment deleted by author.' : comment.content}
                            </Typography>
                            <Stack direction="row" spacing={1} sx={{mt: 1.5}} alignItems="center">
                                {!comment.deleted && (
                                    <Button
                                        size="small"
                                        variant="text"
                                        startIcon={<ReplyOutlinedIcon fontSize="small"/>}
                                        onClick={() => {
                                            setEditingCommentId(null);
                                            setActiveReplyId(current => current === comment.id ? null : comment.id);
                                        }}
                                    >
                                        {replyOpen ? 'Cancel reply' : 'Reply'}
                                    </Button>
                                )}
                                {comment.ownedByCurrentUser && !comment.deleted && (
                                    <Button
                                        size="small"
                                        variant="text"
                                        startIcon={<EditOutlinedIcon fontSize="small"/>}
                                        onClick={() => {
                                            setActiveReplyId(null);
                                            setEditingCommentId(current => current === comment.id ? null : comment.id);
                                            setEditDrafts(previous => ({...previous, [comment.id]: previous[comment.id] ?? comment.content}));
                                        }}
                                    >
                                        {editOpen ? 'Cancel edit' : 'Edit'}
                                    </Button>
                                )}
                                {comment.ownedByCurrentUser && !comment.deleted && (
                                    <Button
                                        size="small"
                                        color="error"
                                        variant="text"
                                        startIcon={<DeleteOutlineIcon fontSize="small"/>}
                                        onClick={() => handleDeleteComment(comment.id)}
                                        disabled={isDeletingComment}
                                    >
                                        {isDeletingComment ? 'Deleting' : 'Delete'}
                                    </Button>
                                )}
                                {comment.replies.length > 0 && (
                                    <Button
                                        size="small"
                                        variant="text"
                                        color="inherit"
                                        startIcon={repliesCollapsed ? <ExpandMoreIcon fontSize="small"/> : <ExpandLessIcon fontSize="small"/>}
                                        onClick={() => setCollapsedReplyIds(previous => ({
                                            ...previous,
                                            [comment.id]: !previous[comment.id]
                                        }))}
                                    >
                                        {repliesCollapsed ? 'Expand' : 'Collapse'} {comment.replies.length} {comment.replies.length === 1 ? 'reply' : 'replies'}
                                    </Button>
                                )}
                            </Stack>
                            {editOpen && !comment.deleted && (
                                <Stack spacing={1.25} sx={{mt: 2}}>
                                    <TextField
                                        label="Edit your comment"
                                        multiline
                                        minRows={2}
                                        value={editDraft}
                                        onChange={(event) => setEditDrafts(previous => ({...previous, [comment.id]: event.target.value}))}
                                        fullWidth
                                    />
                                    <Stack direction="row" spacing={1}>
                                        <Button
                                            variant="contained"
                                            onClick={() => handleEditSubmit(comment.id)}
                                            disabled={isEditingComment || !editDraft.trim()}
                                        >
                                            {isEditingComment ? 'Saving changes' : 'Save changes'}
                                        </Button>
                                        <Button variant="text" onClick={() => setEditingCommentId(null)}>
                                            Cancel
                                        </Button>
                                    </Stack>
                                </Stack>
                            )}
                            {replyOpen && !comment.deleted && (
                                <Stack spacing={1.25} sx={{mt: 2}}>
                                    <TextField
                                        label={`Reply to ${comment.authorDisplayName}`}
                                        multiline
                                        minRows={2}
                                        value={replyDraft}
                                        onChange={(event) => setReplyDrafts(previous => ({...previous, [comment.id]: event.target.value}))}
                                        fullWidth
                                    />
                                    <Box>
                                        <Button
                                            variant="contained"
                                            endIcon={<SendOutlinedIcon/>}
                                            onClick={() => handleReplySubmit(comment.id)}
                                            disabled={isReplySubmitting || !replyDraft.trim()}
                                        >
                                            {isReplySubmitting ? 'Posting reply' : 'Post reply'}
                                        </Button>
                                    </Box>
                                </Stack>
                            )}
                        </Box>
                    </Stack>
                </Paper>

                {comment.replies.length > 0 && !repliesCollapsed && (
                    <Stack
                        spacing={1.5}
                        sx={{
                            ml: {xs: 1, md: 2},
                            pl: replyIndent,
                            borderLeft: '2px solid rgba(88, 64, 162, 0.16)'
                        }}
                    >
                        {comment.replies.map(reply => renderComment(reply, depth + 1))}
                    </Stack>
                )}
            </Stack>
        );
    }

    if (!game) {
        return (
            <Box sx={{display: 'flex', justifyContent: 'center', width: '100%', pt: 16}}>
                <CircularProgress/>
            </Box>
        );
    }

    const communityRatingValue = Number(game.wishlistTotal || '0') > 0 ? parseRatingValue(game.averageRating) : null;
    const communityRatingVisuals = getRatingVisuals(communityRatingValue);
    const quickStats = [
        {label: 'Publisher', value: game.provider || 'Unknown'},
        {label: 'Platforms', value: game.platforms || 'Unknown'},
        {label: 'Interactions', value: game.interactions || '0'},
        {label: 'Wishlist entries', value: game.wishlistTotal || '0'},
        {label: 'Community rating', value: formatTenPointRating(communityRatingValue)},
        {label: 'Metacritic', value: 'Unavailable from current sync'}
    ];

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <PopupMessage message={message} severity={severity}/>
            <Container maxWidth="lg">
                <Paper
                    sx={{
                        overflow: 'hidden',
                        borderRadius: '2rem',
                        background: 'rgba(255,255,255,0.94)',
                        boxShadow: '0 30px 80px rgba(24, 7, 51, 0.26)'
                    }}
                >
                    <Box
                        sx={{
                            position: 'relative',
                            minHeight: {xs: 340, md: 440},
                            p: {xs: 3, md: 5},
                            backgroundImage: `linear-gradient(180deg, rgba(10,7,24,0.08), rgba(10,7,24,0.88)), url(${game.imageUrl})`,
                            backgroundSize: 'cover',
                            backgroundPosition: 'center'
                        }}
                    >
                        <Stack
                            direction={{xs: 'column', xl: 'row'}}
                            spacing={3}
                            justifyContent="space-between"
                            alignItems={{xs: 'flex-start', xl: 'flex-end'}}
                            sx={{width: '100%', mt: 'auto'}}
                        >
                            <Box sx={{maxWidth: 760, color: 'white'}}>
                                <Typography variant="overline" sx={{letterSpacing: '0.14em', opacity: 0.92}}>
                                    {game.provider || 'PlayStation Catalog'}
                                </Typography>
                                <Typography
                                    variant="h2"
                                    fontWeight={900}
                                    sx={{
                                        mb: 1,
                                        color: '#fff',
                                        mixBlendMode: {xs: 'normal', md: 'difference'},
                                        textShadow: '0 6px 28px rgba(0,0,0,0.52)'
                                    }}
                                >
                                    {game.name}
                                </Typography>
                                <Typography
                                    sx={{
                                        opacity: 0.92,
                                        maxWidth: 680,
                                        fontSize: {xs: '1rem', md: '1.05rem'},
                                        textShadow: '0 2px 16px rgba(0,0,0,0.35)',
                                        display: '-webkit-box',
                                        overflow: 'hidden',
                                        textOverflow: 'ellipsis',
                                        WebkitBoxOrient: 'vertical',
                                        WebkitLineClamp: {xs: 3, md: 4}
                                    }}
                                >
                                    {game.description || 'No description is available for this title yet.'}
                                </Typography>
                            </Box>

                            <Paper
                                sx={{
                                    minWidth: {xs: '100%', sm: 280},
                                    maxWidth: 340,
                                    p: 3,
                                    borderRadius: '1.75rem',
                                    color: communityRatingVisuals.text,
                                    background: communityRatingVisuals.background,
                                    border: `1px solid ${communityRatingVisuals.border}`,
                                    boxShadow: '0 24px 70px rgba(15, 23, 42, 0.22)',
                                    backdropFilter: 'blur(14px)'
                                }}
                            >
                                <Stack spacing={1.5}>
                                    <Stack direction="row" spacing={1.25} alignItems="center">
                                        <StarRoundedIcon sx={{color: communityRatingVisuals.icon, fontSize: 36}}/>
                                        <Box>
                                            <Typography variant="overline" sx={{letterSpacing: '0.14em', color: communityRatingVisuals.text}}>
                                                Community Score
                                            </Typography>
                                            <Typography variant="body2" sx={{color: communityRatingVisuals.text}}>
                                                {communityRatingVisuals.label}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                    <Typography variant="h2" fontWeight={900} sx={{lineHeight: 1, color: communityRatingVisuals.text}}>
                                        {formatTenPointRating(communityRatingValue)}
                                    </Typography>
                                    <Typography variant="body2" sx={{color: communityRatingVisuals.text}}>
                                        Built from user list ratings. {Number(game.wishlistTotal || '0')} wishlist entries are tracking this game.
                                    </Typography>
                                </Stack>
                            </Paper>
                        </Stack>
                    </Box>

                    <Box sx={{p: {xs: 3, md: 4}}}>
                        <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5} sx={{mb: 4}}>
                            <Button
                                variant="contained"
                                onClick={() => setModalOpen(true)}
                                startIcon={<PlaylistAddIcon/>}
                            >
                                {game.inWishlist ? 'Edit list entry' : 'Add to my list'}
                            </Button>
                            <Button
                                variant="outlined"
                                onClick={() => game.inWishlist ? handleRemove() : handleQuickAdd()}
                                startIcon={game.inWishlist ? <DeleteOutlineIcon/> : <PlaylistAddIcon/>}
                            >
                                {game.inWishlist ? 'Remove from list' : 'Quick add plan to play'}
                            </Button>
                            {game.url && (
                                <Button
                                    component="a"
                                    href={game.url}
                                    target="_blank"
                                    rel="noreferrer"
                                    variant="text"
                                    endIcon={<OpenInNewIcon/>}
                                >
                                    Open source record
                                </Button>
                            )}
                        </Stack>

                        <Stack direction={{xs: 'column', lg: 'row'}} spacing={3} alignItems="stretch">
                            <Paper
                                sx={{
                                    p: 3,
                                    borderRadius: '1.5rem',
                                    flex: 2,
                                    background: 'linear-gradient(180deg, rgba(250,248,255,0.96), rgba(241,245,255,0.92))',
                                    border: '1px solid rgba(83, 56, 158, 0.08)',
                                    boxShadow: '0 18px 48px rgba(32, 12, 63, 0.08)'
                                }}
                            >
                                <Stack spacing={2}>
                                    <Stack spacing={1}>
                                        <Typography variant="h5" fontWeight={800}>About this game</Typography>
                                        <Typography color="text.secondary">
                                            Narrative, release context, and catalog details for this entry.
                                        </Typography>
                                    </Stack>
                                    <Typography color="text.secondary" sx={{lineHeight: 1.75}}>
                                        {game.description || 'No description is available for this title yet.'}
                                    </Typography>
                                    <Divider/>
                                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                        <Chip
                                            label={formatReleaseDate(game.releaseDate)}
                                            variant="outlined"
                                            sx={{
                                                backgroundColor: 'rgba(91, 33, 182, 0.08)',
                                                color: '#4c1d95',
                                                borderColor: 'rgba(91, 33, 182, 0.18)',
                                                '& .MuiChip-label': {fontWeight: 700, color: '#4c1d95'}
                                            }}
                                        />
                                        <Chip
                                            label={game.platforms || 'Unknown platform'}
                                            variant="outlined"
                                            sx={{
                                                backgroundColor: 'rgba(37, 99, 235, 0.08)',
                                                color: '#1d4ed8',
                                                borderColor: 'rgba(37, 99, 235, 0.18)',
                                                '& .MuiChip-label': {fontWeight: 700, color: '#1d4ed8'}
                                            }}
                                        />
                                        {game.categories && (
                                            <Chip
                                                label={game.categories}
                                                variant="outlined"
                                                sx={{
                                                    backgroundColor: 'rgba(15, 23, 42, 0.04)',
                                                    color: '#334155',
                                                    borderColor: 'rgba(100, 116, 139, 0.16)',
                                                    '& .MuiChip-label': {fontWeight: 700, color: '#334155'}
                                                }}
                                            />
                                        )}
                                    </Stack>
                                    <Stack spacing={1.25}>
                                        <Box>
                                            <Typography variant="body2" sx={{fontWeight: 800, color: '#312e81'}}>
                                                Categories
                                            </Typography>
                                            <Typography color="text.secondary">
                                                {game.categories || 'No categories were supplied by the upstream catalog yet.'}
                                            </Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="body2" sx={{fontWeight: 800, color: '#312e81'}}>
                                                Last updated
                                            </Typography>
                                            <Typography color="text.secondary">
                                                {formatDateTime(game.lastModified)}
                                            </Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="body2" sx={{fontWeight: 800, color: '#312e81'}}>
                                                Added to database
                                            </Typography>
                                            <Typography color="text.secondary">
                                                {formatDateTime(game.created)}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                </Stack>
                            </Paper>

                            <Paper
                                sx={{
                                    p: 3,
                                    borderRadius: '1.5rem',
                                    flex: 1,
                                    background: 'linear-gradient(180deg, rgba(255,255,255,0.96), rgba(243,238,255,0.9))',
                                    border: '1px solid rgba(83, 56, 158, 0.08)',
                                    boxShadow: '0 18px 48px rgba(32, 12, 63, 0.08)'
                                }}
                            >
                                <Stack spacing={2}>
                                    <Stack spacing={1}>
                                        <Typography variant="h5" fontWeight={800}>Quick stats</Typography>
                                        <Typography color="text.secondary">
                                            Snapshot metrics and catalog signals at a glance.
                                        </Typography>
                                    </Stack>
                                    <Box
                                        sx={{
                                            display: 'grid',
                                            gap: 1.25,
                                            gridTemplateColumns: {xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: '1fr'}
                                        }}
                                    >
                                        {quickStats.map(stat => (
                                            <Paper
                                                key={stat.label}
                                                variant="outlined"
                                                sx={{
                                                    p: 1.6,
                                                    borderRadius: '1rem',
                                                    background: 'rgba(255,255,255,0.68)',
                                                    borderColor: 'rgba(109, 72, 170, 0.12)'
                                                }}
                                            >
                                                <Typography variant="body2" sx={{fontWeight: 800, color: '#312e81'}}>
                                                    {stat.label}
                                                </Typography>
                                                <Typography color="text.secondary" sx={{mt: 0.4}}>
                                                    {stat.value}
                                                </Typography>
                                            </Paper>
                                        ))}
                                    </Box>
                                </Stack>
                            </Paper>
                        </Stack>
                    </Box>
                </Paper>

                <Paper sx={{p: {xs: 3, md: 4}, borderRadius: '2rem', mt: 4}}>
                    <Stack spacing={3}>
                        <Box>
                            <Typography variant="h4" fontWeight={900}>Discussion</Typography>
                            <Typography color="text.secondary">
                                Drop a quick reaction or note for this game. New comments show up immediately.
                            </Typography>
                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 1.5}}>
                                <Chip
                                    icon={<ForumOutlinedIcon fontSize="small"/>}
                                    label={`${game.comments || '0'} comments`}
                                    variant="outlined"
                                />
                            </Stack>
                        </Box>

                        <Stack spacing={1.5}>
                            <TextField
                                label="Share your thoughts"
                                multiline
                                minRows={3}
                                value={commentDraft}
                                onChange={(event) => setCommentDraft(event.target.value)}
                                fullWidth
                            />
                            <Box>
                                <Button
                                    variant="contained"
                                    endIcon={<SendOutlinedIcon/>}
                                    onClick={handleCommentSubmit}
                                    disabled={postingTargetId === 'root' || !commentDraft.trim()}
                                >
                                    {postingTargetId === 'root' ? 'Posting comment' : 'Post comment'}
                                </Button>
                            </Box>
                        </Stack>

                        <Divider/>

                        {commentsLoading ? (
                            <Box sx={{display: 'flex', justifyContent: 'center', py: 3}}>
                                <CircularProgress size={26}/>
                            </Box>
                        ) : comments.length === 0 ? (
                            <Typography color="text.secondary">
                                No comments yet. Be the first to add one.
                            </Typography>
                        ) : (
                            <Stack spacing={2}>
                                {comments.map(comment => renderComment(comment))}
                                <Box ref={commentsSentinelRef} sx={{display: 'flex', justifyContent: 'center', py: comments.length > 0 ? 1 : 0}}>
                                    {loadingMoreComments ? (
                                        <CircularProgress size={24}/>
                                    ) : commentsHasMore ? (
                                        <Typography color="text.secondary">Scroll for more comments</Typography>
                                    ) : comments.length > 0 ? (
                                        <Typography color="text.secondary">You’ve reached the end of the discussion.</Typography>
                                    ) : null}
                                </Box>
                            </Stack>
                        )}
                    </Stack>
                </Paper>

                {modalOpen && (
                    <ListModificationModal
                        open={modalOpen}
                        onClose={() => setModalOpen(false)}
                        gameId={game.id}
                        updateGameInWishlist={(updatedGameId, inWishlist) => {
                            if (updatedGameId === game.id) {
                                setGame({...game, inWishlist});
                            }
                        }}
                        onActionComplete={(nextMessage, nextSeverity) => {
                            setMessage(nextMessage);
                            setSeverity(nextSeverity);
                        }}
                    />
                )}
            </Container>
        </Box>
    );
}

export default GameMainPage;
